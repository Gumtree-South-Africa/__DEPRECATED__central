package com.ecg.replyts.core.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
@EnableAutoConfiguration
@Import(ConsulConfigBootstrapConfiguration.class)
public class CloudDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudDiscoveryConfiguration.class);

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = Collections.unmodifiableMap(Stream.of(
      new AbstractMap.SimpleEntry<>("cassandra", "persistence.cassandra.endpoint"),
      new AbstractMap.SimpleEntry<>("elasticsearch", "search.es.endpoints"),
      new AbstractMap.SimpleEntry<>("graphite", "graphite.endpoint.hostname")
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

            discoveryClient.getInstances(service).forEach(instance -> instances.add(instance.getHost()+":"+instance.getPort()));

            if (instances.size() > 0) {
                LOG.info("Auto-discovered {} {} instance(s) - adding to property {}", instances.size(), service, property);
                LOG.debug("Instance(s)  {}", instances);

                gatheredProperties.put(property, StringUtils.collectionToDelimitedString(instances, ","));
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
}
