package com.ecg.replyts.core.runtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;

import java.util.*;

@Configuration
@PropertySource(name = ParentConfiguration.CONF_DIR_PROPERTY_SOURCE, value = "file:${confDir}/replyts.properties")
@Import({ CloudDiscoveryConfiguration.class, LoggingService.class })
public class ParentConfiguration {
    static final String CONF_DIR_PROPERTY_SOURCE = "confDirProperties";

    @Autowired
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void environmentProperties() {
        Map<String, String> environmentProperties = new HashMap<>();

        Optional.ofNullable(System.getenv("COMAAS_HTTP_PORT")).ifPresent(port -> environmentProperties.put("replyts.http.port", port));
        Optional.ofNullable(System.getenv("COMAAS_HAZELCAST_PORT")).ifPresent(port -> environmentProperties.put("hazelcast.port", port));

        if (environmentProperties.size() > 0) {
            environment.getPropertySources().addFirst(new MapPropertySource("Environment-detected properties", Collections.unmodifiableMap(environmentProperties)));
        }
    }
}
