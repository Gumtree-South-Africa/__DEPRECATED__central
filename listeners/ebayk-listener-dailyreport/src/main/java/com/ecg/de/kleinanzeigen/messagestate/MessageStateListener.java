package com.ecg.de.kleinanzeigen.messagestate;

import com.ecg.de.kleinanzeigen.hadoop.HadoopEventEmitter;
import com.ecg.de.kleinanzeigen.hadoop.HadoopLogEntry;
import com.ecg.de.kleinanzeigen.hadoop.TrackerLogStyleUseCase;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mhuttar
 */
public class MessageStateListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessageStateListener.class);

    private static final MessageToEventName EVENT_NAMER = new MessageToEventName();
    private final HadoopEventEmitter hadoopEmitter;

    public MessageStateListener(HadoopEventEmitter hadoopEmitter) {
        this.hadoopEmitter = hadoopEmitter;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            // quite a lot of messages that go through replyts. don't process messages if info logger is not enabled.
            String msg = EVENT_NAMER.jsonLogEntry(message);
            HadoopLogEntry entry = new HadoopLogEntry(TrackerLogStyleUseCase.DAILY_REPORT_V3, msg);
            hadoopEmitter.insert(entry);
        } catch (Exception e) {
            LOG.error("sending to kafka failed", e);
        }
    }
}
