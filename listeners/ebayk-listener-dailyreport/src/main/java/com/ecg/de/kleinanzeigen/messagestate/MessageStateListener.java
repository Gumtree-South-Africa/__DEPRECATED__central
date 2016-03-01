package com.ecg.de.kleinanzeigen.messagestate;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mhuttar
 */
public class MessageStateListener implements MessageProcessedListener {

    private final Logger LOG = LoggerFactory.getLogger("hadoop_daily_report_v2");

    private final static MessageToEventName EVENT_NAMER = new MessageToEventName();

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (LOG.isInfoEnabled()) {
            // quite a lot of messages that go through replyts. don't process messages if info logger is not enabled.
            LOG.info(EVENT_NAMER.jsonLogEntry(message));
        }
    }
}
