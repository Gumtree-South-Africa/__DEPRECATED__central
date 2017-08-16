package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.de.kleinanzeigen.replyts.volumefilter.monitoring.VolumeFilterMonitoringConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(VolumeFilterMonitoringConfiguration.class)
public class VolumeFilterConfiguration {

    @Bean
    public FilterFactory volumeFilterFactory(HazelcastInstance hazelcast, EventStreamProcessor eventStreamProcessor) {
        return new VolumeFilterFactory(hazelcast, eventStreamProcessor);
    }

    @Bean
    public EventStreamProcessor eventStreamProcessor() {
        return new EventStreamProcessor();
    }
}
