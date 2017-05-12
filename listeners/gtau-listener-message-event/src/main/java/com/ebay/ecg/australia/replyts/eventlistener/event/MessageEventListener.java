package com.ebay.ecg.australia.replyts.eventlistener.event;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MessageEventListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessageEventListener.class);

    @Autowired
    private RTSRMQEventCreator eventCreator;

    @PostConstruct
    public void onStartup() {
        LOG.info("MessageEventListener created.");
    }

    public void messageProcessed(Conversation conversation, Message message) {
        try {
            eventCreator.messageEventEntry(conversation, message);
        } catch (RuntimeException e) {
            LOG.error("Message logging failed",e);
        }
    }
}
