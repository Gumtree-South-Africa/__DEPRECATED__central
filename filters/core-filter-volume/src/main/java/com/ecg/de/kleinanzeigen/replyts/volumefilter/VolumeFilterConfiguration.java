package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class VolumeFilterConfiguration {

    @Bean
    public FilterFactory volumeFilterFactory(@Qualifier("cassandraSessionForCore") Session session,
                                             @Value("${filter.volume.max.occurrences.allowed:10000}") int maxAllowedRegisteredOccurrences) {
        return new VolumeFilterFactory(session, maxAllowedRegisteredOccurrences);
    }
}
