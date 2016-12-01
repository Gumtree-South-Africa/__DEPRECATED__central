package com.ecg.de.kleinanzeigen.messagelogging;

import com.ecg.de.kleinanzeigen.hadoop.HadoopEventEmitter;
import com.ecg.de.kleinanzeigen.hadoop.HadoopLogEntry;
import com.ecg.de.kleinanzeigen.hadoop.TrackerLogStyleUseCase;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by johndavis on 29/11/16.
 */
public class MessageLoggingListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageLoggingListener.class);

    private static final EventCreator EVENT_NAMER = new EventCreator();
    private final HadoopEventEmitter hadoopEmitter;

    public MessageLoggingListener(HadoopEventEmitter hadoopEmitter) {
        this.hadoopEmitter = hadoopEmitter;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            // quite a lot of messages that go through replyts. don't process messages if info logger is not enabled.
            String msg = EVENT_NAMER.jsonLogEntry(conversation, message);
            HadoopLogEntry entry = new HadoopLogEntry(TrackerLogStyleUseCase.MESSAGE_EVENTS_V3, msg);
            hadoopEmitter.insert(entry);
        } catch (RuntimeException e) {
            LOG.error("sending to kafka failed", e);
        }
    }
}
