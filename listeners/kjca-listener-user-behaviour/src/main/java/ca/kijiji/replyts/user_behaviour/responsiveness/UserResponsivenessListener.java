package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.discovery.ServiceDirectory;
import ca.kijiji.replyts.user_behaviour.responsiveness.hystrix.metrics.RTSHystrixCodaHaleMetricsPublisher;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.fs.ResponsivenessFilesystemSink;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.SendResponsivenessToServiceCommand;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.ServiceDirectoryCreator;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.MetricsService;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.netflix.hystrix.strategy.HystrixPlugins;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * Calculates user responsiveness based on conversation history and
 * sends the data to a REST service and to a directory on the filesystem.
 */
@Component
public class UserResponsivenessListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(UserResponsivenessListener.class);

    private final CommandCreator commandCreator;
    private final ResponsivenessCalculator responsivenessCalculator;
    private final ServiceDirectory serviceDirectory;
    private final CloseableHttpClient httpClient;
    private final ResponsivenessFilesystemSink filesystemSink;

    private final Counter noResponsivenessRecordCounter;
    private final boolean enabled;
    private final Timer calculationTimer;

    @Autowired
    public UserResponsivenessListener(
            @Value("${user-behaviour.responsiveness.enabled:false}") boolean enabled,
            ServiceDirectoryCreator serviceDirectoryCreator,
            CloseableHttpClient userBehaviourHttpClient,
            ResponsivenessFilesystemSink filesystemSink
    ) {
        this(enabled, new CommandCreator(), new ResponsivenessCalculator(), serviceDirectoryCreator, userBehaviourHttpClient, filesystemSink);
        HystrixPlugins.getInstance().registerMetricsPublisher(
                new RTSHystrixCodaHaleMetricsPublisher(MetricsService.getInstance().getRegistry())
        );
    }

    // Package-protected to allow testing
    UserResponsivenessListener(
            boolean enabled,
            CommandCreator commandCreator,
            ResponsivenessCalculator responsivenessCalculator,
            ServiceDirectoryCreator serviceDirectoryCreator,
            CloseableHttpClient httpClient,
            ResponsivenessFilesystemSink filesystemSink
    ) {
        this.enabled = enabled;
        this.commandCreator = commandCreator;
        this.responsivenessCalculator = responsivenessCalculator;
        this.serviceDirectory = serviceDirectoryCreator.newServiceDirectory();
        this.httpClient = httpClient;
        this.filesystemSink = filesystemSink;

        noResponsivenessRecordCounter = TimingReports.newCounter("user-behaviour.responsiveness.noRecord");
        calculationTimer = TimingReports.newTimer("user-behaviour.responsiveness.calculation");
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (!enabled) {
            return;
        }

        ResponsivenessRecord record;
        try (Timer.Context ignored = calculationTimer.time()) {
            record = responsivenessCalculator.calculateResponsiveness(conversation, message);
        }

        if (record == null) {
            noResponsivenessRecordCounter.inc();
            return;
        }

        try {
            commandCreator.makeSendToServiceCommand(
                    serviceDirectory,
                    httpClient,
                    conversation.getId() + "/" + message.getId(),
                    record.getUserId(),
                    record.getTimeToRespondInSeconds()).execute();
        } catch (Exception e) {
            LOG.warn("Could not report to service. User id: [{}]. TTR: [{}]. Conv id: [{}]. Msg id: [{}]",
                    record.getUserId(),
                    record.getTimeToRespondInSeconds(),
                    conversation.getId(),
                    message.getId(),
                    e);
        }

        filesystemSink.storeResponsivenessRecord(Thread.currentThread().getName(), record);
    }

    /**
     * Make sure all records are flushed to the filesystem on shutdown.
     */
    @PreDestroy
    void flushFilesystemSink() {
        filesystemSink.flushAll();
    }

    // For testing
    static class CommandCreator {
        SendResponsivenessToServiceCommand makeSendToServiceCommand(
                ServiceDirectory serviceDirectory,
                CloseableHttpClient httpClient,
                String traceHeader,
                long uid,
                int timeToRespondSeconds
        ) {
            return new SendResponsivenessToServiceCommand(serviceDirectory, httpClient, traceHeader, uid, timeToRespondSeconds, false);
        }
    }
}
