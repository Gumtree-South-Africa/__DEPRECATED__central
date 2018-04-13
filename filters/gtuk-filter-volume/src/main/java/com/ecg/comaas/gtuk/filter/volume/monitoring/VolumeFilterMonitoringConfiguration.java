package com.ecg.comaas.gtuk.filter.volume.monitoring;

import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@ConditionalOnProperty(name = "volume-filter.monitoring.enabled", havingValue = "true")
public class VolumeFilterMonitoringConfiguration {
    @Bean
    public SpringContextProvider applicationContext(ApplicationContext context) {
        return new SpringContextProvider("/volume-filter/monitoring", VolumeFilterMonitoringWebConfiguration.class, context);
    }
}
