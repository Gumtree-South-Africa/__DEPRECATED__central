package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource("file:${confDir}/replyts.properties")
@Import(CloudDiscoveryConfiguration.class)
public class ParentConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ParentConfiguration.class);

    @Autowired
    private ConfigurableEnvironment environment;

    @Value("${service.discovery.enabled:false}")
    private boolean discoveryEnabled;

    // This is no longer needed as soon as we move to the cloud. Keep for now
    @PostConstruct
    public void initializePersistenceStrategyIfMissing() {
        // Check if the COMAAS_HTTP_PORT env var was set, and introduce it as the replyts.http.port. If not set, this value
        // should come from the replyts.properties file.
        String comaasHttpPort = System.getenv("COMAAS_HTTP_PORT");
        if (comaasHttpPort != null) {
            environment.getPropertySources().addFirst(new MapPropertySource("Environment-detected properties", ImmutableMap.of("replyts.http.port", comaasHttpPort)));
        }

        if (discoveryEnabled) {
            return;
        }
        if (!environment.containsProperty("persistence.strategy")) {
            String strategy;

            if (Boolean.parseBoolean(environment.getProperty("persistence.riak.enabled"))) {
                strategy = "riak";
            } else if (Boolean.parseBoolean(environment.getProperty("persistence.cassandra.enabled"))) {
                strategy = "cassandra";
            } else {
                throw new IllegalStateException("No persistence (strategy) indicator found; e.g. persistence.strategy, persistence.cassandra.enabled, ..");
            }

            LOG.warn("Found deprecated 'persistence.*.enabled' property - re-writing to 'persistence.strategy'");
            environment.getPropertySources().addFirst(new MapPropertySource("strategy", ImmutableMap.of("persistence.strategy", strategy)));
        }
    }
}
