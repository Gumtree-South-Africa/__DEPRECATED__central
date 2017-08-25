package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.SendResponsivenessToServiceCommand;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink.ResponsivenessSink;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Calculates user responsiveness based on conversation history and
 * sends the data to a REST service and to a directory on the filesystem.
 */
@Component
@ConditionalOnProperty(value = "user-behaviour.responsiveness.enabled", havingValue = "true")
public class UserResponsivenessListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(UserResponsivenessListener.class);

    private final ResponsivenessCalculator responsivenessCalculator;
    private final ResponsivenessSink sink;
    private final SendResponsivenessToServiceCommand sendResponsivenessCommand;
    private final Counter noRecordCounter;
    private final Timer calculationTimer;

    @Autowired
    public UserResponsivenessListener(
            ResponsivenessCalculator responsivenessCalculator,
            ResponsivenessSink sink,
            SendResponsivenessToServiceCommand sendResponsivenessCommand
    ) {
        this.responsivenessCalculator = responsivenessCalculator;
        this.sink = sink;
        this.sendResponsivenessCommand = sendResponsivenessCommand;
        this.noRecordCounter = TimingReports.newCounter("user-behaviour.responsiveness.noRecord");
        this.calculationTimer = TimingReports.newTimer("user-behaviour.responsiveness.calculation");
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        ResponsivenessRecord record = createRecord(conversation, message);
        if (record == null) {
            noRecordCounter.inc();
            return;
        }

        try {
            sendResponsivenessCommand.setResponsivenessRecord(record);
            sendResponsivenessCommand.execute();
        } catch (Exception e) {
            LOG.error("Could not report to service. " + record.toString(), e);
        }

        sink.storeRecord(Thread.currentThread().getName(), record);
    }

    private ResponsivenessRecord createRecord(Conversation conversation, Message message) {
        try (Timer.Context ignored = calculationTimer.time()) {
            return responsivenessCalculator.calculateResponsiveness(conversation, message);
        }
    }
}
