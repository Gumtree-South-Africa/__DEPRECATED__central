package com.ecg.comaas.ebayk.listener.dailyreport.messagestate;

import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopEventEmitter;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopLogEntry;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.TrackerLogStyleUseCase;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
