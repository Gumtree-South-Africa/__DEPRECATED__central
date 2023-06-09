package com.ecg.replyts.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.config.ConsulConfigBootstrapConfiguration;
import org.springframework.cloud.consul.config.ConsulPropertySourceLocator;
import org.springframework.cloud.consul.discovery.ConsulLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

@Configuration
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = { BusAutoConfiguration.class, DataSourceAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class })
@Import(ConsulConfigBootstrapConfiguration.class)
public class CloudConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudConfiguration.class);

    @Autowired
    private ConsulLifecycle lifecycle;

    @Autowired
    private ConsulPropertySourceLocator consulPropertySourceLocator;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private LoggingService loggingService;

    @Value("${tenant}")
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

        // This will add properties from Consul KV
        discoverProperties();

        logProperties();

        // This will no longer be needed once we can use e.g. @RefreshScope
        loggingService.initialize();
    }

    private void discoverProperties() {
        LOG.info("Auto-discovering cloud properties and services");

        environment.getPropertySources().addAfter(ComaasPropertySource.NAME, consulPropertySourceLocator.locate(environment));

    }

    private void logProperties() {
        Iterator<org.springframework.core.env.PropertySource<?>> iterator = environment.getPropertySources().iterator();

        SortedMap<String, Object> effectiveProperties = new TreeMap<>();

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

}
