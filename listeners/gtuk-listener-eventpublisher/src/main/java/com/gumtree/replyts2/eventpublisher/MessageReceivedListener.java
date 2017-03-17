package com.gumtree.replyts2.eventpublisher;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.gumtree.replyts2.common.message.MessageCenterUtils;
import com.gumtree.replyts2.common.message.MessageTextHandler;
import com.gumtree.replyts2.eventpublisher.event.MessageReceivedEvent;
import com.gumtree.replyts2.eventpublisher.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Optional;

public class MessageReceivedListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageReceivedListener.class);
    private static final int MAX_CHARS = 250;

    private EventPublisher rabbitEventPublisher;
    private boolean pluginEnabled;

    @Autowired
    public MessageReceivedListener(EventPublisher rabbitEventPublisher, boolean pluginEnabled) {
        this.rabbitEventPublisher = rabbitEventPublisher;
        this.pluginEnabled = pluginEnabled;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if(pluginEnabled) {
            switch (message.getState()) {

                case SENT: rabbitEventPublisher.publishEvent(getEvent(conversation, message)
                        .orElseThrow(() -> new IllegalStateException("Event should not be null")));
                    break;

                default: break;//Do nothing
            }
        }

    }

    private Optional<MessageReceivedEvent> getEvent(Conversation conversation, Message message) {
        try {
            Assert.notNull(conversation, "Conversation cannot be null");
            Assert.notNull(message, "Message cannot be null");

            return Optional.of(new MessageReceivedEvent.Builder()
                    .setAdvertId(Long.parseLong(conversation.getAdId()))
                    .setConversationId(conversation.getId())
                    .setMessageDirection(message.getMessageDirection())
                    .setBuyerEmail(conversation.getBuyerId())
                    .setSellerEmail(conversation.getSellerId())
                    .setText(preparePushText(message))
                    .build());
        } catch (Exception e) {
            LOG.error("En error occurred while creating the event");
            return null;
        }
    }

    private String preparePushText(Message message) {
        return MessageCenterUtils.truncateText(MessageTextHandler.remove(message.getPlainTextBody()), MAX_CHARS);
    }
}
