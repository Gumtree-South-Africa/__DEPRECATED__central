package com.ecg.australia.replyts.eventlistener.event;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageEventListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventListener.class);

    private final RTSRMQEventCreator eventCreator;

    @Autowired
    public MessageEventListener(RTSRMQEventCreator eventCreator) {
        this.eventCreator = eventCreator;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (conversation == null || message == null) {
            LOG.error("Either conversation or message is null -> conversation={}, message={}", conversation, message);
            return;
        }
        eventCreator.messageEventEntry(conversation, message);
    }
}
