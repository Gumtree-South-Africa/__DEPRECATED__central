package com.ecg.de.kleinanzeigen.replyts.volumefilter.monitoring;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;

@ConditionalOnProperty(name = "volume-filter.monitoring.enabled", havingValue = "true")
public class VolumeFilterMonitoringConfiguration {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EmbeddedWebserver webserver;

    @PostConstruct
    public void context() {
        webserver.context(new SpringContextProvider("/volume-filter/monitoring", VolumeFilterMonitoringWebConfiguration.class, context));
    }
}
