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
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final CloseableHttpClient httpClient;
    private final HttpHost httpHost;
    private final HystrixCommand.Setter userBehaviourHystrixConfig;
    private final Counter noRecordCounter;
    private final Timer calculationTimer;

    @Autowired
    public UserResponsivenessListener(
            ResponsivenessCalculator responsivenessCalculator,
            ResponsivenessSink sink,
            CloseableHttpClient httpClient,
            @Qualifier("userBehaviourHystrixConfig") HystrixCommand.Setter userBehaviourHystrixConfig,
            @Value("${user-behaviour.responsiveness.http.endpoint:user-behaviour-service.clworker.qa10.kjdev.ca}") String httpEndpoint,
            @Value("${user-behaviour.responsiveness.http.port:80}") Integer httpPort
    ) {
        this.responsivenessCalculator = responsivenessCalculator;
        this.sink = sink;
        this.httpClient = httpClient;
        this.httpHost = new HttpHost(httpEndpoint, httpPort);
        this.userBehaviourHystrixConfig = userBehaviourHystrixConfig;
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
            SendResponsivenessToServiceCommand sendResponsivenessCommand = createHystrixCommand();
            sendResponsivenessCommand.setResponsivenessRecord(record);
            sendResponsivenessCommand.execute();
        } catch (Exception e) {
            LOG.error("Could not report to service. " + record.toString(), e);
        }

        sink.storeRecord(Thread.currentThread().getName(), record);
    }

    // used for testing
    SendResponsivenessToServiceCommand createHystrixCommand() {
        return new SendResponsivenessToServiceCommand(httpClient, userBehaviourHystrixConfig, httpHost);
    }

    private ResponsivenessRecord createRecord(Conversation conversation, Message message) {
        try (Timer.Context ignored = calculationTimer.time()) {
            return responsivenessCalculator.calculateResponsiveness(conversation, message);
        }
    }
}
