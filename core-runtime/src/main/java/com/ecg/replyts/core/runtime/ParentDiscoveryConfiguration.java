package com.ecg.replyts.core.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.config.ConsulConfigBootstrapConfiguration;
import org.springframework.cloud.consul.config.ConsulPropertySourceLocator;
import org.springframework.cloud.consul.discovery.ConsulLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@PropertySource("file:${confDir}/replyts.properties")
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude=FreeMarkerAutoConfiguration.class)
@Import(ConsulConfigBootstrapConfiguration.class)
public class ParentDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ParentDiscoveryConfiguration.class);

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = Collections.unmodifiableMap(Stream.of(
      new AbstractMap.SimpleEntry<>("cassandra", "persistence.cassandra.endpoint"),
      new AbstractMap.SimpleEntry<>("elasticsearch", "search.es.endpoints")
    ).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    @Value("${replyts.http.port:0}")
    private Integer httpPort;

    @Autowired(required = false)
    private ConsulLifecycle lifecycle;

    @Autowired(required = false)
    private ConsulPropertySourceLocator propertySourceLocator;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @PostConstruct
    public void initializeDiscovery() {
        if (lifecycle == null) {
            return;
        }

        // XXX: Temporary fix until we switch to Spring Boot (this jumpstarts the lifecycle which fetches the properties)

        lifecycle.onApplicationEvent(new EmbeddedServletContainerInitializedEvent(
                new EmbeddedWebApplicationContext(),
                new EmbeddedServletContainer() {
                    @Override
                    public void start() throws EmbeddedServletContainerException {
                    }

                    @Override
                    public void stop() throws EmbeddedServletContainerException {
                    }

                    @Override
                    public int getPort() {
                        return httpPort;
                    }
                }
        ));

        LOG.info("Registered service under instance {}", discoveryClient.getLocalServiceInstance().getUri().toString());

        // Auto-discovery any cloud-related services by asking Consul about any known instances

        Map<String, Object> gatheredProperties = new HashMap<>();

        LOG.info("Auto-discovering cloud services and overriding appropriate properties");

        // Ask the auto-discovery mechanism about discoverable service instances

        DISCOVERABLE_SERVICE_PROPERTIES.forEach((service, property) -> {
            List<String> instances = new ArrayList<>();

            discoveryClient.getInstances(service).forEach(instance -> instances.add(instance.getHost() + ":" + instance.getPort()));

            if (instances.size() > 0) {
                String instanceList = StringUtils.collectionToDelimitedString(instances, ",");

                LOG.info("Auto-discovered {} {} instance(s) - adding property {} = {}", instances.size(), service, property, instanceList);

                gatheredProperties.put(property, instanceList);
            } else {
                LOG.info("Auto-discovered 0 {} instance(s) - not populating property {}", service, property);
            }
        });

        environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", gatheredProperties));

        // Initialize the property source locator if KV-lookups are enabled (service.configuration.enabled)

        if (propertySourceLocator != null) {
            environment.getPropertySources().addFirst(propertySourceLocator.locate(environment));
        }
    }

    @PostConstruct
    public void initializePersistenceStrategyIfMissing() {
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

            Map<String, Object> propertyMap = Collections.unmodifiableMap(Stream.of(
                    new AbstractMap.SimpleEntry<>("persistence.strategy", strategy)
            ).collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

            environment.getPropertySources().addFirst(new MapPropertySource("strategy", propertyMap));
        }
    }
}
