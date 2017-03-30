package com.ebay.ecg.australia.replyts.eventlistener.event;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fmiri on 24/03/2017.
 */
public class MessageEventListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventListener.class);

    private RTSRMQEventCreator eventCreator;

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

    public RTSRMQEventCreator getEventCreator() {
        return eventCreator;
    }

    public void setEventCreator(RTSRMQEventCreator eventCreator) {
        this.eventCreator = eventCreator;
    }
}
