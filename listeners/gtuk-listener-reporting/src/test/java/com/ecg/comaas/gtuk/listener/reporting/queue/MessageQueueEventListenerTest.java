package com.ecg.comaas.gtuk.listener.reporting.queue;

import com.ecg.comaas.gtuk.listener.reporting.ConversationBuilder;
import com.ecg.comaas.gtuk.listener.reporting.MessageBuilder;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.util.Clock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


/**
 * Author: bpadhiar
 */
public class MessageQueueEventListenerTest {

    private MessageQueueManager messageQueueManager;
    private Clock clock;
    private Date testDate;

    @Before
    public void setup() {
        messageQueueManager = mock(MessageQueueManager.class);
        clock = mock(Clock.class);
        testDate = new DateTime(2015, 5, 30, 1, 0, 0).toDate();
        when(clock.now()).thenReturn(testDate);

    }

    @Test
    public void conversationHasNoCategoryId() {
        MessageQueueEventListener messageQueueEventListener = new MessageQueueEventListener(messageQueueManager, clock);

        MessageBuilder messageBuilder = getDefaultMessageBuilder();
        Message message = messageBuilder.createMessage();

        ConversationBuilder conversationBuilder = getDefaultConversationBuilder();
        conversationBuilder.addMessage(message);
        Conversation conversation = conversationBuilder.createConversation();

        messageQueueEventListener.messageProcessed(conversation, message);

        verifyZeroInteractions(messageQueueManager);
    }

    @Test
    public void messageIsNotHeldAndHasNoHumanInteraction() {
        MessageQueueEventListener messageQueueEventListener = new MessageQueueEventListener(messageQueueManager, clock);

        MessageBuilder messageBuilder = getDefaultMessageBuilder();
        messageBuilder.humanResultState(ModerationResultState.UNCHECKED)
                .messageState(MessageState.SENT)
                .filterResultState(FilterResultState.OK);
        Message message = messageBuilder.createMessage();

        ConversationBuilder conversationBuilder = getDefaultConversationBuilder();
        conversationBuilder.addMessage(message);
        conversationBuilder.customCategoryId("1234");
        Conversation conversation = conversationBuilder.createConversation();

        messageQueueEventListener.messageProcessed(conversation, message);

        verifyZeroInteractions(messageQueueManager);
    }

    @Test
    public void messageIsHeldAndHasNoHumanInteraction() {
        MessageQueueEventListener messageQueueEventListener = new MessageQueueEventListener(messageQueueManager, clock);

        MessageBuilder messageBuilder = getDefaultMessageBuilder();
        messageBuilder.humanResultState(ModerationResultState.UNCHECKED)
                .messageState(MessageState.HELD)
                .filterResultState(FilterResultState.HELD);
        Message message = messageBuilder.createMessage();

        ConversationBuilder conversationBuilder = getDefaultConversationBuilder();
        conversationBuilder.addMessage(message);
        conversationBuilder.customCategoryId("1234");
        Conversation conversation = conversationBuilder.createConversation();

        messageQueueEventListener.messageProcessed(conversation, message);

        verify(messageQueueManager).enQueueMessage(eq("testMessageId"), eq(1234L), eq(testDate));
        verifyNoMoreInteractions(messageQueueManager);
    }

    @Test
    public void messageIsApprovedByHumanInteraction() {
        MessageQueueEventListener messageQueueEventListener = new MessageQueueEventListener(messageQueueManager, clock);

        MessageBuilder messageBuilder = getDefaultMessageBuilder();
        messageBuilder.humanResultState(ModerationResultState.GOOD)
                .messageState(MessageState.SENT)
                .filterResultState(FilterResultState.HELD);
        Message message = messageBuilder.createMessage();

        ConversationBuilder conversationBuilder = getDefaultConversationBuilder();
        conversationBuilder.addMessage(message);
        conversationBuilder.customCategoryId("1234");
        Conversation conversation = conversationBuilder.createConversation();

        messageQueueEventListener.messageProcessed(conversation, message);

        verify(messageQueueManager).deQueueMessage(eq("testMessageId"));
        verifyNoMoreInteractions(messageQueueManager);
    }

    private MessageBuilder getDefaultMessageBuilder() {
        return new MessageBuilder()
                .messageId("testMessageId")
                .messageDirection(MessageDirection.BUYER_TO_SELLER)
                .messageState(MessageState.HELD)
                .messageReceivedAt(new DateTime(2015, 5, 30, 1, 0, 0))
                .messageLastModifiedAt(new DateTime(2015, 5, 30, 2, 0, 0))
                .filterResultState(FilterResultState.OK)
                .humanResultState(ModerationResultState.UNCHECKED);
    }

    private ConversationBuilder getDefaultConversationBuilder() {

        return new ConversationBuilder()
                .conversationId("testConversationId")
                .conversationState(ConversationState.ACTIVE)
                .advertId("123456789")
                .sellerId("foo@bar.com")
                .buyerId("bar@foo.com")
                .createdAt(new DateTime(2015, 5, 30, 0, 0, 0))
                .lastModifiedAt(new DateTime(2015, 5, 30, 0, 1, 0));
    }


}