package com.ecg.replyts.core.runtime;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.LayoutBase;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.github.danielwegener.logback.kafka.encoding.LayoutKafkaMessageEncoder;
import com.github.danielwegener.logback.kafka.keying.RoundRobinKeyingStrategy;
import com.google.common.collect.ImmutableMap;
import net.logstash.logback.layout.LogstashAccessLayout;
import net.logstash.logback.layout.LogstashLayout;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@PropertySource("discovery.properties")
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = { FreeMarkerAutoConfiguration.class, DataSourceAutoConfiguration.class, BusAutoConfiguration.class, HazelcastAutoConfiguration.class })
@Import(ConsulConfigBootstrapConfiguration.class)
@ConditionalOnExpression("#{'${service.discovery.enabled:false}' == 'true'}")
public class CloudDiscoveryConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CloudDiscoveryConfiguration.class);

    private static final String LOGGER_APPENDER_KAFKA_ENABLED = "service.discovery.logger.appender.enabled";
    private static final String LOGGER_APPENDER_KAFKA_LOGS_TOPIC = "service.discovery.logger.appender.logs.topic";
    private static final String LOGGER_APPENDER_KAFKA_ACCESS_LOGS_TOPIC = "service.discovery.logger.appender.access.logs.topic";

    private static final Map<String, String> DISCOVERABLE_SERVICE_PROPERTIES = ImmutableMap.of(
      "persistence.cassandra.core.endpoint", "cassandra",
      "persistence.cassandra.mb.endpoint", "cassandra",
      "search.es.endpoints", "elasticsearch"
    );

    private static final String LOG_APPENDER_SERVICE = "kafkalog";

    @Autowired
    private ConsulLifecycle lifecycle;

    @Autowired
    private ConsulPropertySourceLocator propertySourceLocator;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    private KafkaAppender accessLogAppender = new KafkaAppender();

    @Bean
    public KafkaAppender accessLogAppender() {
        return accessLogAppender;
    }

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

        // This will introduce a tenant, version, etc. field to be sent along with log lines
        mdcInjection();

        // Consul may override the (Kafka) logging related properties, so fetch them from the Environment here rather
        // than through @Value annotations at the top
        if (Boolean.valueOf(environment.getProperty(LOGGER_APPENDER_KAFKA_ENABLED, "false"))) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            // Add ROOT logging via Kafka

            String logsTopic = environment.getProperty(LOGGER_APPENDER_KAFKA_LOGS_TOPIC);

            if (StringUtils.hasText(logsTopic)) {
                KafkaAppender appender = createKafkaAppender(context, logsTopic);

                context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
            }

            // Also complete the relevant settings of the Kafka appender for access logs (must be done after @Bean
            // has initialized so that any related properties can be read from Consul)

            String accessLogsTopic = environment.getProperty(LOGGER_APPENDER_KAFKA_ACCESS_LOGS_TOPIC);

            if (StringUtils.hasText(accessLogsTopic)) {
                LogstashAccessLayout layout = new LogstashAccessLayout();

                layout.setCustomFields(mdcToJson()); // Necessary as this layout doesn't have MDC support

                fillInKafkaAppender(context, accessLogsTopic, accessLogAppender, layout);
            }
        }
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
        environment.getPropertySources().addAfter(ParentConfiguration.CONF_DIR_PROPERTY_SOURCE, kvPropertySource);
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

    private KafkaAppender createKafkaAppender(LoggerContext context, String topic) {
        KafkaAppender appender = new KafkaAppender();

        fillInKafkaAppender(context, topic, appender, new LogstashLayout());
        ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel("INFO");
        appender.addFilter(filter);

        return appender;
    }

    private void fillInKafkaAppender(LoggerContext context, String topic, KafkaAppender appender, LayoutBase layout) {
        List<String> instances = new ArrayList<>();

        discoveryClient.getInstances(LOG_APPENDER_SERVICE).forEach(instance -> instances.add(instance.getHost() + ":" + instance.getPort()));

        if (instances.size() > 0) {
            String instance = instances.get(0);
            LOG.info("Auto-discovered {} Kafka instance(s) to be used for the '{}' log appender - will use the first one: {}", instances.size(), topic, instance);

            layout.setContext(context);
            layout.start();

            appender.setEncoder(new LayoutKafkaMessageEncoder(layout, Charset.forName("UTF-8")));
            appender.setName(topic + "COMaaSKafkaAppender");
            appender.setTopic(topic);
            appender.setContext(context);
            appender.addProducerConfigValue("bootstrap.servers", instance);
            appender.setKeyingStrategy(new RoundRobinKeyingStrategy());
            appender.setDeliveryStrategy(new AsynchronousDeliveryStrategy());
            appender.start();
        } else {
            LOG.info("Auto-discovered 0 Kafka instance(s) - not adding Kafka-based ROOT log appender");
        }
    }

    private void mdcInjection() {
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

    private String mdcToJson() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("{ ");

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        if (contextMap.size() > 0) {
            contextMap.forEach((key, value) -> mdcToJsonField(buffer, key, value));
            buffer.delete(buffer.length() - 2, buffer.length());
        }

        buffer.append(" }");

        return buffer.toString();
    }

    private void mdcToJsonField(StringBuffer buffer, String key, String value) {
        buffer.append('"');
        buffer.append(StringEscapeUtils.escapeJson(key));
        buffer.append("\": \"");
        buffer.append(StringEscapeUtils.escapeJson(value));
        buffer.append("\", ");
    }
}