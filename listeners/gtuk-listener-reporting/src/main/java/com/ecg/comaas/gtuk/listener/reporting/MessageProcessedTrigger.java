package com.ecg.comaas.gtuk.listener.reporting;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import org.joda.time.DateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageProcessedTrigger {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private DataWarehouseEventLogListener listener;

    public MessageProcessedTrigger(DataWarehouseEventLogListener listener) {
        this.listener = listener;
    }

    public void run() {
        final Runnable job = new Runnable() {
            @Override
            public void run() {
                try {
                    Message message = new MessageBuilder()
                            .messageId("testMessageId")
                            .messageDirection(MessageDirection.BUYER_TO_SELLER)
                            .messageState(MessageState.SENT)
                            .messageReceivedAt(new DateTime(2015, 5, 30, 1, 0, 0))
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
                            .addMessage(message)
                            .createConversation();

                    listener.messageProcessed(conversation, message);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        scheduler.scheduleAtFixedRate(job, 5, 5, TimeUnit.SECONDS);
    }
}
