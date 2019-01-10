package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class TestRemoteFilter {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0); // 0 = dynamic port
    private ObjectMapper objectMapper = new ObjectMapper();

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


    public static com.ecg.comaas.filterapi.dto.FilterResponse buildExampleResponse() {
        return new com.ecg.comaas.filterapi.dto.FilterResponse()
                .feedback(
                        Collections.singletonList(
                                new com.ecg.comaas.filterapi.dto.FilterFeedback()
                                        .description("desc")
                                        .resultState(com.ecg.comaas.filterapi.dto.FilterFeedback.ResultStateEnum.OK)
                                        .score(100)
                                        .uiHint("UI Hint")
                        )
                );
    }

    private void startMockEndpoint(Duration responseDelay) {
        try {
            WireMock.stubFor(post(urlEqualTo(endpointPath))
                    .willReturn(
                            aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(objectMapper.writeValueAsBytes(buildExampleResponse()))
                                    .withFixedDelay((int) responseDelay.toMillis())
                    )
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void remoteFilterResponds() throws MalformedURLException {
        startMockEndpoint(Duration.ofMillis(0));

        RemoteFilter remoteFilter = new RemoteFilter(null, endpointURL);

        List<FilterFeedback> actualResult = remoteFilter.filter(CtxMocker.createMinimallyMockedContext());

        List<FilterFeedback> expResult = FilterAPIMapper.FromAPI.toFilterFeedback(buildExampleResponse()).get();
        assertEquals(expResult, actualResult);
    }


    @Test // should have some (timeout = 3000) but this blows us up in magic ways. Add it, and if it works, keep it.
    public void remoteFilterIsInterruptible() {
        startMockEndpoint(Duration.ofMillis(10000)); // a slow response, which we will interrupt

        RemoteFilter remoteFilter = new RemoteFilter(null, endpointURL);
        MultiThreadingTestUtil.assertThatCallableIsInterruptible(
                () -> remoteFilter.filter(CtxMocker.createMinimallyMockedContext()),
                InterruptibleFilter::isFilterInterruptException,
                Duration.ofMillis(1000)
        );
    }

    private static class CtxMocker {
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

            ctx.setConversation(mockMutableConversation(msg));
            System.out.println("CONVERSATION: " + ctx.getConversation());

            return ctx;
        }

        public static Message mockMessage() {
            Message msg = Mockito.mock(Message.class);
            when(msg.getId()).thenReturn((UUID.randomUUID().toString()));
            when(msg.getCaseInsensitiveHeaders()).thenReturn(Collections.EMPTY_MAP);
            when(msg.getPlainTextBody()).thenReturn("The plaintext body ");
            when(msg.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
            return msg;
        }

        public static Mail mockMail() {
            Mail mail = Mockito.mock(Mail.class);
            when(mail.getSubject()).thenReturn("The Subject");
            return mail;
        }

        public static MutableConversation mockMutableConversation(Message msg) {
            // yup, MessageProcessingContext.setConversation() requires a MutableConversation
            MutableConversation conv = Mockito.mock(MutableConversation.class);
            when(conv.getUserId(anyObject())).thenReturn("someUserId");
            when(conv.getMessageById(anyString())).thenReturn(msg);
            when(conv.getImmutableConversation()).thenReturn(conv);
            return conv;
        }

    }
}
