package com.ecg.au.gumtree.messagelogging;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mhuttar
 */
public class MessageLoggingListener implements MessageProcessedListener {

    private final Logger LOG = LoggerFactory.getLogger("message-json-log");

    private static final Logger ERR_LOG = LoggerFactory.getLogger(MessageLoggingListener.class);

    private final static EventCreator EVENT_NAMER = new EventCreator();

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            if (LOG.isInfoEnabled()) {
                // quite a lot of messages that go through replyts. don't process messages if info logger is not enabled.
                LOG.info(EVENT_NAMER.jsonLogEntry(conversation, message));
            }
        } catch (RuntimeException e) {
            ERR_LOG.error("Message logging failed",e);
        }
    }
}
