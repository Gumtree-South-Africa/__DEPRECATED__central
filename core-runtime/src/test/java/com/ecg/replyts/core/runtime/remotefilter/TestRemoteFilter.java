package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.comaas.filterapi.dto.FilterRequest;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class TestRemoteFilter {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0); // 0 = dynamic port

    private String endpointPath = "/filter/ek/" + UUID.fromString("5da68dfe-6492-4243-87b9-79b4d2763f09").toString();

    private String mockHost = "http://localhost";

    private URL endpointURL;

    @Before
    public void setup() throws MalformedURLException {
        this.endpointURL = UriBuilder.fromUri(mockHost)
                .port(wireMockRule.port())
                .path(endpointPath)
                .build()
                .toURL();
    }

    public static com.ecg.comaas.filterapi.dto.FilterResponse exampleResponse = new com.ecg.comaas.filterapi.dto.FilterResponse()
            .feedback(
                    Collections.singletonList(
                            new com.ecg.comaas.filterapi.dto.FilterFeedback()
                                    .description("desc")
                                    .resultState(com.ecg.comaas.filterapi.dto.FilterFeedback.ResultStateEnum.OK)
                                    .score(100)
                                    .uiHint("UI Hint")
                    )
            );

    private void startMockEndpoint(Duration responseDelay) {
        try {
            WireMock.stubFor(post(urlEqualTo(endpointPath))
                    .willReturn(
                            aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(FilterAPIMapper.getSerializer().writeValueAsBytes(exampleResponse))
                                    .withFixedDelay((int) responseDelay.toMillis())
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    // helper for any method that works with a mocked MessageProcessingContext, trying to save the poor dev that has to deal with it's failures in the future
    public static <T> T executeCatchingNPE(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (NullPointerException e) {
            /**
             * Unfortunately we have to mock the MessageProcessingContext, which will throw an NPE if a method-mock is not implemented.
             * If that happens and the test fails on some NPE thrown from within the System Under Test.
             *
             * However: the fault is on the mocking in this test, and this will not be clear to the developer seeing the failed test.
             * So document this in the output. You're welcome!
             *
             * Solution: Make sure your MessageProcessingContext has sufficient state (e.g. by mocking the appropriate method).
             *
             * Note:
             * - there is Mockito.RETURNS_SMART_NULLS for this, but that doesn't work with primitives and finals (e.g. String)
             * - I tried some other ways to solve like Mockito.mock(Message.class, createSomeCustomizedAnswerThatThrows()) but this had other problems.
             *
             */
            throw new RuntimeException(
                    "NullPointException was thrown (see cause), but this was possibly a fault of the test setup / mock. Read the test source! ",
                    e
            );
        }
    }

    @Test
    public void remoteFilterResponds() {
        startMockEndpoint(Duration.ofMillis(0));

        RemoteFilter remoteFilter = RemoteFilter.create(endpointURL);

        List<FilterFeedback> actualResult = executeCatchingNPE(() -> remoteFilter.filter(CtxMocker.createMinimallyMockedContext()));

        List<FilterFeedback> expResult = FilterAPIMapper.FromAPI.toFilterFeedback(exampleResponse).get();
        assertEquals(expResult, actualResult);
    }

    @Test
    public void jsonDateTimeIsCorrectlySerialized() throws JsonProcessingException {
        // the generated class do not annotate the DateTime values with
        // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        // or so. That's unfortunate, because now we have to do that elsewhere (e.g. in objectmapper).

        MessageProcessingContext ctx = CtxMocker.createMinimallyMockedContext();

        FilterRequest filterRequest = executeCatchingNPE(() ->
                FilterAPIMapper.FromModel.toFilterRequest(ctx, "correlationId", 1000)
        );

        // execute the correct serializer
        String data = FilterAPIMapper.getSerializer().writeValueAsString(filterRequest);

        assertTrue(
                "Serialization should contain " + CtxMocker.receivedAtDateTime + ", but doesn't: " + data,
                data.contains(CtxMocker.receivedAtDateTime)
        );
    }


    @Test // should have some (timeout = 3000) but this blows us up in magic ways. Add it, and if it works, keep it.
    public void remoteFilterIsInterruptible() {
        startMockEndpoint(Duration.ofMillis(10000)); // a slow response, which we will interrupt

        RemoteFilter remoteFilter = RemoteFilter.create(endpointURL);
        MultiThreadingTestUtil.assertThatCallableIsInterruptible(
                () -> executeCatchingNPE(() -> remoteFilter.filter(CtxMocker.createMinimallyMockedContext())),
                InterruptibleFilter::isFilterInterruptException,
                Duration.ofMillis(1000)
        );
    }

    private static class CtxMocker {
        private static final String receivedAtDateTime = "2008-09-15T15:53:00Z";

        /**
         * Mail and Conversation are pretty big beasts of which we only need a few elements, so
         * that's why they are mocked with Mockito, providing only minimal functionaly.
         */
        public static MessageProcessingContext createMinimallyMockedContext() {
            Message msg = mockMessage();
            MessageProcessingContext ctx = new MessageProcessingContext(
                    mockMail(),
                    msg.getId(),
                    new ProcessingTimeGuard(12)
            );

            // this is not part of the constructor, but apparently Filters can/should assume this conversation has been set in the context.
            ctx.setConversation(mockMutableConversation(msg));
            System.out.println("CONVERSATION: " + ctx.getConversation());
            return ctx;
        }

        public static Message mockMessage() {
            Mockito.mock(Message.class);
            Message msg = Mockito.mock(Message.class);
            when(msg.getId()).thenReturn((UUID.randomUUID().toString()));
            when(msg.getCaseInsensitiveHeaders()).thenReturn(Collections.EMPTY_MAP);
            when(msg.getPlainTextBody()).thenReturn("The plaintext body ");
            when(msg.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
            when(msg.getReceivedAt()).thenReturn(DateTime.parse(receivedAtDateTime));
            return msg;
        }

        public static Mail mockMail() {
            Mail mail = Mockito.mock(Mail.class);
            when(mail.getSubject()).thenReturn("The Subject");
            when(mail.makeMutableCopy()).thenReturn(null);
            return mail;
        }

        public static MutableConversation mockMutableConversation(Message msg) {
            // yup, MessageProcessingContext.setConversation() requires a MutableConversation, not any Conversation
            MutableConversation conv = Mockito.mock(MutableConversation.class);
            when(conv.getUserId(anyObject())).thenReturn("someUserId");
            when(conv.getMessageById(anyString())).thenReturn(msg);
            when(conv.getImmutableConversation()).thenReturn(conv);
            when(conv.getId()).thenReturn("conversationId1234");
            return conv;
        }

    }
}
