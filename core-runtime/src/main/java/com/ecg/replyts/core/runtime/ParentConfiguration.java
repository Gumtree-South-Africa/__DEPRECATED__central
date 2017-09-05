package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableMap;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.TENANT;

@Configuration
@PropertySource(name = ParentConfiguration.CONF_DIR_PROPERTY_SOURCE, value = "file:${confDir}/replyts.properties")
@Import(CloudDiscoveryConfiguration.class)
public class ParentConfiguration {

    static final String CONF_DIR_PROPERTY_SOURCE = "confDirProperties";

    @Autowired
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void postConstruct() {
        MDC.put(TENANT, environment.getProperty("replyts.tenant", "unknown"));

        String comaasHttpPort = System.getenv("COMAAS_HTTP_PORT");
        if (comaasHttpPort != null) {
            environment.getPropertySources().addFirst(new MapPropertySource("Environment-detected properties", ImmutableMap.of("replyts.http.port", comaasHttpPort)));
        }
    }
}
