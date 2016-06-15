package com.ecg.replyts.core.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@PropertySource("file:${confDir}/replyts.properties")
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration
@Import(ConsulConfigBootstrapConfiguration.class)
public class CloudDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudDiscoveryConfiguration.class);

    private static final String INSTANCE_CASSANDRA = "cassandra";
    private static final String PROPERTY_CASSANDRA_ENDPOINT = "persistence.cassandra.endpoint";

    @Value("#{'${persistence.riak.enabled:false}' ? 'riak' : 'cassandra'}")
    private String conversationRepositorySource;

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
    @ConditionalOnBean(DiscoveryClient.class)
    public void initializeDiscovery() {
        if (lifecycle == null) {
            // don't try to do anything with Consul
            return;
        }
        // Initialize the Consul lifecycle explicitly to register the service and make it discoverable

        // XXX: Temporary fix until we switch to the Spring Boot webserver

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

        // Ask the auto-discovery mechanism about Cassandra instances

        if (conversationRepositorySource.equals(INSTANCE_CASSANDRA)) {
            List<String> instances = new ArrayList<>();

            discoveryClient.getInstances(conversationRepositorySource).forEach(instance -> instances.add(instance.getHost()));

            if (instances.size() > 0) {
                LOG.info("Auto-discovered {} instances - adding to property {}", instances.size(), PROPERTY_CASSANDRA_ENDPOINT);

                gatheredProperties.put(PROPERTY_CASSANDRA_ENDPOINT, StringUtils.collectionToDelimitedString(instances, ","));
            } else {
                LOG.info("Auto-discovered 0 instances - not populating property {}", PROPERTY_CASSANDRA_ENDPOINT);
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", gatheredProperties));

        // Initialize the property source locator if KV-lookups are enabled (service.configuration.enabled)

        if (propertySourceLocator != null) {
            environment.getPropertySources().addFirst(propertySourceLocator.locate(environment));
        }
    }
}
