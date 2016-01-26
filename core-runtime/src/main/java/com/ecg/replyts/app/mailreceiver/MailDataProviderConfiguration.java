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
    private final MessageProcessingCoordinator coordinator;

    @Autowired
    public MailDataProviderConfiguration(
            @Value("${mailreceiver.filesystem.dropfolder}") String mailDataDir,
            @Value("${mailreceiver.retrydelay.minutes}") int retryDelay,
            @Value("${mailreceiver.retries}") int retryCounter,
            MessageProcessingCoordinator coordinator
    ) {
        this.mailDataDir = mailDataDir;
        this.retryDelay = retryDelay;
        this.retryCounter = retryCounter;
        this.coordinator = coordinator;
    }

    @Bean
    public MailDataProvider buildMailDataProvider(ClusterModeManager clusterModeManager) {

        MailDataProvider provider = new FilesystemMailDataProvider(new File(mailDataDir), retryDelay, retryCounter, coordinator, clusterModeManager);

        return provider;
    }

}
