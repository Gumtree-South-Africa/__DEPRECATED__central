package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
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
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = FreeMarkerAutoConfiguration.class)
@Import(ConsulConfigBootstrapConfiguration.class)
@ConditionalOnExpression("#{'${service.configuration.enabled:false}' == 'true' || '${service.discovery.enabled:false}' == 'true'}")
public class CloudDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudDiscoveryConfiguration.class);

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = ImmutableMap.of(
            "cassandra", "persistence.cassandra.endpoint",
            "elasticsearch", "search.es.endpoints"
    );

    @Value("${replyts.tenant:generic}")
    private String tenant;

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

        LOG.info("Auto-discovering cloud services and overriding appropriate properties");
        environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", discoverServices()));

        // Initialize the property source locator if KV-lookups are enabled (service.configuration.enabled)
        if (propertySourceLocator != null) {
            environment.getPropertySources().addFirst(propertySourceLocator.locate(environment));
        }
    }

    Map<String, Object> discoverServices() {
        Map<String, Object> gatheredProperties = new HashMap<>();
        DISCOVERABLE_SERVICE_PROPERTIES.forEach((service, property) -> {
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
