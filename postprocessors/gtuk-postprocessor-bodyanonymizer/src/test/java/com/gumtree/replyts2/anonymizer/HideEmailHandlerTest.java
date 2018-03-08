package com.gumtree.replyts2.anonymizer;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import com.ecg.replyts.core.runtime.mailparser.StringTypedContentMime4J;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.FilterResultState.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by reweber on 07/10/15
 */
@RunWith(MockitoJUnitRunner.class)
public class HideEmailHandlerTest {
    private static final String BUYER_EMAIL = "from@email.com";
    private static final String BUYER_SECRET = "b.secret";
    private static final String BUYER_EMAIL_ANON = "b.secret@rts.kijiji.ca";
    private static final String SELLER_EMAIL = "to@email.com";
    private static final String SELLER_SECRET = "s.secret";
    private static final String SELLER_EMAIL_ANON = "s.secret@rts.kijiji.ca";

    private static final String CLOAKEMAIL_HEADER = "X-Cust-Emailaddresscloak";
    private static final String CLOAKEMAIL_VALUE = "REVEAL";

    @Mock
    private MessageProcessingContext context;

    @Mock
    private MutableMail outgoingMail;

    @Mock
    private MultiTennantMailCloakingService mailCloakingService;

    @Mock
    private MutableMail incomingMail;

    @Mock
    MessageBodyAnonymizerConfig messageBodyAnonymizerConfig;

    private HideEmailHandler  hideEmailHandler;
    private ImmutableConversation.Builder conversationBuilder;
    private ImmutableMessage.Builder messageBuilder;

    @Before
    public void setUp() throws Exception {
        when(context.getOutgoingMail()).thenReturn(outgoingMail);
        messageBuilder = ImmutableMessage.Builder
                .aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.UNDECIDED)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withFilterResultState(OK)
                .withHumanResultState(ModerationResultState.UNCHECKED)
                .withHeaders(ImmutableMap.of())
                .withTextParts(Collections.singletonList(""))
                .withProcessingFeedback(ImmutableList.of());

        conversationBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId("cid")
                .withState(ConversationState.ACTIVE)
                .withBuyer(BUYER_EMAIL, BUYER_SECRET)
                .withSeller(SELLER_EMAIL, SELLER_SECRET)
                .withMessages(ImmutableList.of(messageBuilder.build()));

        when(context.getMail()).thenReturn(Optional.of(incomingMail));

        when(messageBodyAnonymizerConfig.getRevealEmailHeader()).thenReturn(CLOAKEMAIL_HEADER);
        when(messageBodyAnonymizerConfig.getRevealEmailValue()).thenReturn(CLOAKEMAIL_VALUE);

        hideEmailHandler = new HideEmailHandler(messageBodyAnonymizerConfig, mailCloakingService);
    }

    @Test
    public void messageShouldNotBeAnonymized_notChanged() throws Exception {
        Map<String, String> headers = ImmutableMap.of("X-Cust-Emailaddresscloak", "REVEAL");
        Message message = mock(Message.class);
        when(message.getHeaders()).thenReturn(headers);
        when(context.getMessage()).thenReturn(message);

        hideEmailHandler.process(context);

        verify(context).getMessage();
        verifyNoMoreInteractions(outgoingMail, context, mailCloakingService);
    }

    @Test
    public void messageDoesNotContainAddresses_notChanged() throws Exception {
        String part1String = "Nothing matches in this part";
        Entity mockEntity1 = new MockEntity();
        Body body1 = mock(Body.class);
        mockEntity1.setBody(body1);
        TypedContent<String> part1 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part1String, mockEntity1);
        List<TypedContent<String>> list = ImmutableList.of(part1);
        when(outgoingMail.getTextParts(false)).thenReturn(list);
        ImmutableConversation conversation = conversationBuilder.build();
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMessage()).thenReturn(mock(Message.class));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress(BUYER_EMAIL_ANON));

        hideEmailHandler.process(context);

        assertThat(part1.getContent(), is(part1String));
    }

    @Test
    public void messageHasBuyersRealEmail_replacedWithAnonVersion() throws Exception {
        String part1String = "Email me at " + BUYER_EMAIL;
        Entity mockEntity1 = new MockEntity();
        Body body1 = mock(Body.class);
        mockEntity1.setBody(body1);
        TypedContent<String> part1 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part1String, mockEntity1);
        List<TypedContent<String>> list = ImmutableList.of(part1);
        when(outgoingMail.getTextParts(false)).thenReturn(list);
        ImmutableConversation conversation = conversationBuilder.build();
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMessage()).thenReturn(mock(Message.class));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress(BUYER_EMAIL_ANON));

        hideEmailHandler.process(context);

        assertThat(part1.getContent(), is("Email me at " + BUYER_EMAIL_ANON));
    }

    @Test
    public void messageHasSellersRealEmail_replacedWithAnonVersion() throws Exception {
        String part1String = "Email me at " + SELLER_EMAIL;
        Entity mockEntity1 = new MockEntity();
        Body body1 = mock(Body.class);
        mockEntity1.setBody(body1);
        TypedContent<String> part1 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part1String, mockEntity1);
        List<TypedContent<String>> list = ImmutableList.of(part1);
        when(outgoingMail.getTextParts(false)).thenReturn(list);
        ImmutableConversation conversation = conversationBuilder.build();
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMessage()).thenReturn(mock(Message.class));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress(SELLER_EMAIL_ANON));

        hideEmailHandler.process(context);

        assertThat(part1.getContent(), is("Email me at " + SELLER_EMAIL_ANON));
    }

    @Test
    public void messageHasTwoParts_sellersRealEmail_replacedWithAnonVersionInBothParts() throws Exception {
        String part1String = "Email me at " + SELLER_EMAIL;
        String part2String = String.format("Email me at <a href='mailto:%s'>%s</a>", SELLER_EMAIL, SELLER_EMAIL);

        Entity mockEntity1 = new MockEntity();
        Body body1 = mock(Body.class);
        mockEntity1.setBody(body1);
        TypedContent<String> part1 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part1String, mockEntity1);

        Entity mockEntity2 = new MockEntity();
        Body body2 = mock(Body.class);
        mockEntity2.setBody(body2);
        TypedContent<String> part2 = new StringTypedContentMime4J(MediaType.HTML_UTF_8, part2String, mockEntity2);

        List<TypedContent<String>> list = ImmutableList.of(part1, part2);

        when(outgoingMail.getTextParts(false)).thenReturn(list);
        ImmutableConversation conversation = conversationBuilder.build();
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMessage()).thenReturn(mock(Message.class));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress(SELLER_EMAIL_ANON));

        hideEmailHandler.process(context);

        assertThat(part1.getContent(), is("Email me at " + SELLER_EMAIL_ANON));
        assertThat(part2.getContent(), is(String.format("Email me at <a href='mailto:%s'>%s</a>", SELLER_EMAIL_ANON, SELLER_EMAIL_ANON)));
    }

    private static class MockEntity implements Entity {

        private Body body;

        @Override
        public Entity getParent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setParent(Entity parent) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Header getHeader() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setHeader(Header header) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Body getBody() {
            return body;
        }

        @Override
        public void setBody(Body body) {
            this.body = body;
        }

        @Override
        public Body removeBody() {
            Body old = body;
            body = null;
            return old;
        }

        @Override
        public boolean isMultipart() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getMimeType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCharset() {
            return "UTF-8";
        }

        @Override
        public String getContentTransferEncoding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getDispositionType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getFilename() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}