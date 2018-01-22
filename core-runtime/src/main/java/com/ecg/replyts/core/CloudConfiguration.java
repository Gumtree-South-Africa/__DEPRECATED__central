package com.ecg.replyts.core;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.config.ConsulConfigBootstrapConfiguration;
import org.springframework.cloud.consul.config.ConsulPropertySourceLocator;
import org.springframework.cloud.consul.discovery.ConsulLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = {BusAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Import(ConsulConfigBootstrapConfiguration.class)
@ConditionalOnExpression("#{'${service.discovery.enabled:true}' == 'true'}")
public class CloudConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudConfiguration.class);

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = ImmutableMap.of(
      "persistence.cassandra.core.endpoint", "cassandra",
      "persistence.cassandra.mb.endpoint", "cassandra",
      "queue.kafka.endpoint", "kafkacore",
      "kafka.core.servers", "kafkacore",
      "search.es.endpoints", "elasticsearch"
    );

    @Autowired
    private ConsulLifecycle lifecycle;

    @Autowired
    private ConsulPropertySourceLocator propertySourceLocator;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private LoggingService loggingService;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    @PostConstruct
    private void initializeDiscovery() {
        // XXX: Temporary fix until we switch to Spring Boot (this jump starts the lifecycle which fetches the properties)

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
                        // This breaks registration in Consul, on purpose, so it cannot be accidentally turned on in discovery.properties.
                        return 0;
                    }
                }
        ));

        // This will add properties from Consul KV and select Consul services
        discoverProperties();

        // This will not longer be needed once we can use e.g. @RefreshScope
        loggingService.initialize();
    }

    private void discoverProperties() {
        LOG.info("Auto-discovering cloud properties and services");

        org.springframework.core.env.PropertySource<?> kvPropertySource = propertySourceLocator.locate(environment);
        Map<String, Object> discoveredProperties = discoverServices();

        discoveredProperties.forEach((key, value) -> {
            if (kvPropertySource.containsProperty(key)) {
                LOG.warn("Auto-discovered service {} already exists in Consul KV store - using value from Consul KV store: {}", key, kvPropertySource.getProperty(key));
            }
        });

        // Cloud properties take precedence over discovered services
        environment.getPropertySources().addAfter(ComaasPropertySource.NAME, kvPropertySource);
        environment.getPropertySources().addAfter(kvPropertySource.getName(), new MapPropertySource("discoveredServicesProperties", discoverServices()));

        Iterator<org.springframework.core.env.PropertySource<?>> iterator = environment.getPropertySources().iterator();

        Map<String, Object> effectiveProperties = new HashMap<>();

        for (int i = 0; iterator.hasNext(); i++) {
            org.springframework.core.env.PropertySource<?> source = iterator.next();

            LOG.info("Property source #{} = {} (\"{}\")", i, source.getClass().getName(), source.getName());

            if (source instanceof EnumerablePropertySource) {
                Arrays.stream(((EnumerablePropertySource) source).getPropertyNames())
                        .forEach(name -> effectiveProperties.put(name, source.getProperty(name)));
            } else {
                throw new IllegalStateException("Could not enumerate property source #{}!");
            }
        }

        effectiveProperties.forEach((key, value) -> LOG.info("Property {} = {}", key, value));
    }

    private Map<String, Object> discoverServices() {
        Map<String, Object> gatheredProperties = new HashMap<>();

        DISCOVERABLE_SERVICE_PROPERTIES.forEach((property, service) -> {
            LOG.info("Discovering service {} for property {}", service, property);

            List<ServiceInstance> discoveredInstances = discoveryClient.getInstances(service);

            List<String> instances = discoveredInstances.stream()
                    .filter(instance -> instance.getMetadata().containsKey("tenant-" + tenant))
                    .peek(instance -> LOG.debug("Discovered tenant specific service {}", instance))
                    .map(instance -> instance.getHost() + ":" + instance.getPort())
                    .collect(Collectors.toList());

            if (instances.size() == 0 && discoveredInstances.size() > 0) {
                instances = discoveredInstances.stream()
                        .filter(instance -> instance.getMetadata().keySet().stream().filter(key -> key.startsWith("tenant-")).count() == 0)
                        .peek(instance -> LOG.debug("Discovered shared service {}", instance))
                        .map(instance -> instance.getHost() + ":" + instance.getPort())
                        .collect(Collectors.toList());
            }

            if (instances.size() > 0) {
                LOG.info("Auto-discovered {} {} instance(s) - adding property {}", instances.size(), service, property);

                gatheredProperties.put(property, instances.stream().collect(Collectors.joining(",")));
            } else {
                LOG.info("Auto-discovered 0 {} instance(s) - not populating property {}", service, property);
            }
        });

        return gatheredProperties;
    }
}
