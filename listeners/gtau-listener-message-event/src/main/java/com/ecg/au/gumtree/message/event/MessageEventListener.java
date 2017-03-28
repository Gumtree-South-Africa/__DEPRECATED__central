package com.ecg.au.gumtree.message.event;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author mhuttar
 */
public class MessageEventListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventListener.class);
    private static final RTSRMQEventCreator RTSRMQ_EVENT_CREATOR = new RTSRMQEventCreator();

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            RTSRMQ_EVENT_CREATOR.messageEventEntry(conversation, message);
        } catch (RuntimeException e) {
            LOG.error("Message logging failed",e);
        }
    }
}
