package com.gumtree.comaas.filter.flagged;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.gumtree.MockFactory;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.UserFlaggedFilterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.gumtree.MockFactory.mockConversation;
import static com.gumtree.MockFactory.mockMessage;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeUserFlaggedFilterTest.TestContext.class)
public class GumtreeUserFlaggedFilterTest {
    private static final String BUYER_EMAIL = "buyer@hotmail.com";
    private static final String SELLER_EMAIL = "seller@hotmail.com";

    @Autowired
    private GumtreeUserFlaggedFilter filter;

    @Test
    public void testNoFlaggedConversation() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation(BUYER_EMAIL, SELLER_EMAIL, message);

        List<FilterFeedback> feedback = filter.filter(getMessageProcessingContext(mail, message, conversation));
        assertEquals(0, feedback.size());
    }

    @Test
    public void testFlaggedBySellerMessageSentByBuyer() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = new MockFactory.ConversationBuilder()
                .withBuyer(BUYER_EMAIL).withSeller(SELLER_EMAIL)
                .addMessage(message)
                .addHeader("flagged-seller", "2015-01-01T12:00:00Z")
                .build();

        List<FilterFeedback> feedback = filter.filter(getMessageProcessingContext(mail, message, conversation));
        assertEquals(1, feedback.size());
    }

    @Test
    public void testFlaggedBySellerMessageSentBySeller() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = new MockFactory.ConversationBuilder()
                .withBuyer(BUYER_EMAIL).withSeller(SELLER_EMAIL)
                .addMessage(message)
                .addHeader("flagged-seller", "2015-01-01T12:00:00Z")
                .build();

        List<FilterFeedback> feedback = filter.filter(getMessageProcessingContext(mail, message, conversation));
        assertEquals(1, feedback.size());
    }

    @Test
    public void testFlaggedByBuyerMessageSentBySeller() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = new MockFactory.ConversationBuilder()
                .withBuyer(BUYER_EMAIL).withSeller(SELLER_EMAIL)
                .addMessage(message)
                .addHeader("flagged-buyer", "2015-01-01T12:00:00Z")
                .build();

        List<FilterFeedback> feedback = filter.filter(getMessageProcessingContext(mail, message, conversation));
        assertEquals(1, feedback.size());
    }

    @Test
    public void testFlaggedByBuyerMessageSentByBuyer() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = new MockFactory.ConversationBuilder()
                .withBuyer(BUYER_EMAIL).withSeller(SELLER_EMAIL)
                .addMessage(message)
                .addHeader("flagged-buyer", "2015-01-01T12:00:00Z")
                .build();

        List<FilterFeedback> feedback = filter.filter(getMessageProcessingContext(mail, message, conversation));
        assertEquals(1, feedback.size());
    }

    private MessageProcessingContext getMessageProcessingContext(Mail mail, Message message, MutableConversation conversation) {
        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, message.getId(),
                new ProcessingTimeGuard(1L));
        messageProcessingContext.setConversation(conversation);
        messageProcessingContext.setMessageDirection(message.getMessageDirection());
        return messageProcessingContext;
    }

    @Configuration
    static class TestContext {
        @Bean
        public UserFlaggedFilterConfig filterConfig() throws Exception {
            return new UserFlaggedFilterConfig.Builder(State.ENABLED, 1, null,
                    "flagged-buyer", "flagged-seller")
                    .build();
        }

        @Bean
        public GumtreeUserFlaggedFilter filter(UserFlaggedFilterConfig filterConfig) {
            return new GumtreeUserFlaggedFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig);
        }
    }
}