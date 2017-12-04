package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.datastax.driver.core.Session;
import com.ecg.de.kleinanzeigen.replyts.volumefilter.monitoring.VolumeFilterMonitoringConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@ComaasPlugin
@Configuration
@Import(VolumeFilterMonitoringConfiguration.class)
public class VolumeFilterConfiguration {

    @Bean
    public FilterFactory volumeFilterFactory(SharedBrain sharedBrain, EventStreamProcessor eventStreamProcessor,
                                             @Qualifier("cassandraSessionForCore") Session session,
                                             @Value("${filter.volume.cassandra.implementation.timeout.millis:1000}") long timeoutMillis,
                                             @Value("${filter.volume.cassandra.implementation.enabled:false}") boolean cassandraImplementationEnabled,
                                             @Value("${filter.volume.max.occurrences.allowed:10000}") int maxAllowedRegisteredOccurrences,
                                             @Value("${filter.volume.cassandra.implementaion.threads:${replyts.threadpool.size:10}}") int cassandraImplementationThreads) {
        return new VolumeFilterFactory(sharedBrain, eventStreamProcessor, session, Duration.ofMillis(timeoutMillis),
                cassandraImplementationEnabled, maxAllowedRegisteredOccurrences, cassandraImplementationThreads);
    }

    @Bean
    public EventStreamProcessor eventStreamProcessor() {
        return new EventStreamProcessor();
    }

    @Bean
    public SharedBrain sharedBrain(HazelcastInstance hazelcast, EventStreamProcessor processor) {
        return new SharedBrain(hazelcast, processor);
    }
}
