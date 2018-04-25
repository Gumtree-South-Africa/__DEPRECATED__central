package com.ecg.comaas.ebayk.listener.jsonlogging;

import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopEventEmitter;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopLogEntry;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.TrackerLogStyleUseCase;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Component
public class JsonLoggingListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(JsonLoggingListener.class);

    private static final EventCreator EVENT_NAMER = new EventCreator();
    private final HadoopEventEmitter hadoopEmitter;

    @Autowired
    public JsonLoggingListener(HadoopEventEmitter hadoopEmitter) {
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
