package com.ecg.replyts.app.mailreceiver;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MailDataProviderConfiguration {

    private final String mailDataDir;
    private final int retryDelay;
    private final int retryCounter;
    private final int watchRetryDelay;
    private final MessageProcessingCoordinator coordinator;

    @Autowired
    public MailDataProviderConfiguration(
            @Value("${mailreceiver.filesystem.dropfolder}") String mailDataDir,
            @Value("${mailreceiver.retrydelay.minutes}") int retryDelay,
            @Value("${mailreceiver.retries}") int retryCounter,
            @Value("${mailreceiver.watch.retrydelay.millis:1000}") int watchRetryDelay,
            MessageProcessingCoordinator coordinator
    ) {
        this.mailDataDir = mailDataDir;
        this.retryDelay = retryDelay;
        this.retryCounter = retryCounter;
        this.watchRetryDelay = watchRetryDelay;
        this.coordinator = coordinator;
    }

    /**
     * This Bean requires a ClusterModeManager, which requires a ClusterMonitor, which is a Riak specific Bean.
     * This should be refactored because some tenants (MP) do not use Riak at all but are still wiring these beans. They
     * get around the fact that there are no Riak hosts by setting
     * persistence.riak.datacenter.primary.hosts=localhost
     * in replyts.properties.
     */
    @Bean
    public MailDataProvider buildMailDataProvider(ClusterModeManager clusterModeManager) {
        return new FilesystemMailDataProvider(new File(mailDataDir), retryDelay, retryCounter, watchRetryDelay, coordinator, clusterModeManager);
    }

}
