package com.ecg.replyts.core.runtime;

import ch.qos.logback.classic.LoggerContext;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.github.danielwegener.logback.kafka.encoding.LayoutKafkaMessageEncoder;
import com.github.danielwegener.logback.kafka.keying.RoundRobinKeyingStrategy;
import com.google.common.collect.ImmutableMap;
import net.logstash.logback.layout.LogstashLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = {FreeMarkerAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Import(ConsulConfigBootstrapConfiguration.class)
@ConditionalOnExpression("#{'${service.discovery.enabled:false}' == 'true'}")
public class CloudDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudDiscoveryConfiguration.class);

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = ImmutableMap.of(
            "cassandra", "persistence.cassandra.endpoint",
            "elasticsearch", "search.es.endpoints"
    );

    private static final String LOG_APPENDER_SERVICE = "kafkalog";

    @Autowired
    ConsulLifecycle lifecycle;

    @Autowired(required = false)
    ConsulPropertySourceLocator propertySourceLocator;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    DiscoveryClient discoveryClient;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    @PostConstruct
    void initializeDiscovery() {
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
                        // This is a dummy value and will be overwritten by the properties file or
                        // the COMAAS_HTTP_PORT system env var. The env var has precedence over the properties file var.
                        return 0;
                    }
                }
        ));

        LOG.info("Auto-discovering cloud services and overriding appropriate properties");

        environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", discoverServices()));

        // Initialize the property source locator if KV-lookups are enabled (service.discovery.enabled)

        if (propertySourceLocator != null) {
            environment.getPropertySources().addFirst(propertySourceLocator.locate(environment));
        }

        // Required properties may have been taken from Consul, so defer initializing logging to here

        populateMDC();

        if (Boolean.valueOf(environment.getProperty("service.discovery.logger.appender.enabled", "false"))) {
            String appenderTopic = environment.getProperty("service.discovery.logger.appender.topic");

            addKafkaAppender(appenderTopic);
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

    private void addKafkaAppender(String topic) {
        List<String> instances = new ArrayList<>();

        discoveryClient.getInstances(LOG_APPENDER_SERVICE).forEach(instance -> {
            instances.add(instance.getHost() + ":" + instance.getPort());
        });

        if (instances.size() > 0) {
            LOG.info("Auto-discovered {} Kafka instance(s) to be used for the ROOT log appender - will use the first one", instances.size());

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

            KafkaAppender appender = new KafkaAppender();

            appender.addProducerConfigValue("bootstrap.server", instances.get(0));
            appender.setTopic(topic);
            appender.setKeyingStrategy(new RoundRobinKeyingStrategy());
            appender.setDeliveryStrategy(new AsynchronousDeliveryStrategy());

            LayoutKafkaMessageEncoder encoder = new LayoutKafkaMessageEncoder();

            // Use a Logstash layout for logging; this automatically adds in the MDC fields as well

            LogstashLayout layout = new LogstashLayout();

            layout.setContext(context);

            encoder.setContext(context);
            encoder.setLayout(layout);

            appender.setContext(context);
            appender.setEncoder(encoder);

            layout.start();
            encoder.start();
            appender.start();

            rootLogger.addAppender(appender);
        } else {
            LOG.info("Auto-discovered 0 Kafka instance(s) - not adding Kafka-based ROOT log appender");
        }
    }

    private void populateMDC() {
        try {
            InetAddress address = InetAddress.getLocalHost();

            MDC.put("host", address.getHostName());
            MDC.put("ip", address.getHostAddress());
        } catch (UnknownHostException e) {
            LOG.error("Unable to determine primary IP and/or hostname to place in logger MDC", e);
        }

        MDC.put("version", getClass().getPackage().getImplementationVersion());
        MDC.put("tenant", tenant);
    }

}
