package com.ecg.comaas.gtuk.listener.reporting;

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
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataWarehouseEventLogListenerTest {

    private static final DateTime FIXED_DATE_TIME = new DateTime(2015, 6, 1, 0, 0, 0);

    private EventPublisher jsonEventPublisher;

    private DataWarehouseEventLogListener listener;

    @Before
    public void setup() {
        jsonEventPublisher = Mockito.mock(EventPublisher.class);
        Clock clock = Mockito.mock(Clock.class);
        listener = new DataWarehouseEventLogListener(jsonEventPublisher, clock);

        when(clock.now()).thenReturn(FIXED_DATE_TIME.toDate());
    }


    @Test
    public void writesJsonEventCorrectlyWithCustomValues() throws IOException {

        // Given
        Message message = new MessageBuilder()
                .messageId("testMessageId")
                .messageDirection(MessageDirection.BUYER_TO_SELLER)
                .messageState(MessageState.SENT)
                .messageReceivedAt(new DateTime(2015, 5, 30, 1, 0, 0))
                .messageLastModifiedAt(new DateTime(2015, 5, 30, 2, 0, 0))
                .filterResultState(FilterResultState.OK)
                .humanResultState(ModerationResultState.UNCHECKED)
                .addProcessingFeedback(new ProcessingFeedbackBuilder()
                        .filterName("testFilter1")
                        .filterInstance("testFilter1a")
                        .resultState(FilterResultState.OK)
                        .score(1)
                        .uiHint("testFilter1Hint")
                        .evaluation(true))
                .addProcessingFeedback(new ProcessingFeedbackBuilder()
                        .filterName("testFilter2")
                        .filterInstance("testFilter2b")
                        .resultState(FilterResultState.HELD)
                        .score(2)
                        .uiHint("testFilter2Hint")
                        .evaluation(false))
                .addProcessingFeedback(new ProcessingFeedbackBuilder()
                        .createBogusProcessingFeedback())
                .createMessage();

        Conversation conversation = new ConversationBuilder()
                .conversationId("testConversationId")
                .conversationState(ConversationState.ACTIVE)
                .advertId("123456789")
                .sellerId("foo@bar.com")
                .buyerId("bar@foo.com")
                .createdAt(new DateTime(2015, 5, 30, 0, 0, 0))
                .lastModifiedAt(new DateTime(2015, 5, 30, 0, 1, 0))
                .customCategoryId("1001")
                .customBuyerIp("192.168.0.1")
                .addMessage(Mockito.mock(Message.class))
                .addMessage(Mockito.mock(Message.class))
                .addMessage(message)
                .createConversation();

        // When
        listener.messageProcessed(conversation, message);

        // Then
        MessageProcessedEvent expected = new MessageProcessedEvent.Builder()
                .messageId("testMessageId")
                .conversationId("testConversationId")
                .messageDirection(MessageDirection.BUYER_TO_SELLER)
                .conversationState(ConversationState.ACTIVE)
                .messageState(MessageState.SENT)
                .filterResultState(FilterResultState.OK)
                .humanResultState(ModerationResultState.UNCHECKED)
                .adId(123456789L)
                .sellerMail("foo@bar.com")
                .buyerMail("bar@foo.com")
                .numOfMessageInConversation(2)
                .timestamp(new DateTime(2015, 6, 1, 0, 0, 0))
                .conversationCreatedAt(new DateTime(2015, 5, 30, 0, 0, 0))
                .messageReceivedAt(new DateTime(2015, 5, 30, 1, 0, 0))
                .conversationLastModifiedAt(new DateTime(2015, 5, 30, 0, 1, 0))
                .messageLastModifiedAt(new DateTime(2015, 5, 30, 2, 0, 0))
                .customCategoryId(1001)
                .customBuyerIp("192.168.0.1")
                .addFilterResult(new FilterExecutionResult.Builder()
                        .filterName("testFilter1")
                        .filterInstance("testFilter1a")
                        .resultState(FilterResultState.OK.name())
                        .score(1)
                        .uiHint("testFilter1Hint")
                        .evaluation(true))
                .addFilterResult(new FilterExecutionResult.Builder()
                        .filterName("testFilter2")
                        .filterInstance("testFilter2b")
                        .resultState(FilterResultState.HELD.name())
                        .score(2)
                        .uiHint("testFilter2Hint")
                        .evaluation(false))
                .createMessageProcessedEvent();

        verify(jsonEventPublisher).publish(expected);
    }
}