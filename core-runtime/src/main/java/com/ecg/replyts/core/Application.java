package com.ecg.replyts.core;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.ecg.replyts.core.runtime.HttpServerFactory;
import com.ecg.replyts.core.runtime.StartupExperience;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategy;
import org.bitsofinfo.hazelcast.discovery.consul.DoNothingRegistrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Configuration
@ComponentScan({ "com.ecg.replyts.app", "com.ecg.replyts.core.runtime" })
@ComponentScan(value = { "com.ecg", "com.ebay" }, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(ComaasPlugin.class))
@ImportResource("classpath*:/plugin-inf/*.xml")
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    static {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOG.error("Uncaught exception in thread {}", thread, throwable));

        LoggingService.bootstrap();
    }

    public static final String PRODUCTIVE_PROFILE = "productive";
    public static final String EMBEDDED_PROFILE = "embedded";
    public static final String MIGRATION_PROFILE = "migration";

    @Bean
    public Config hazelcastConfiguration(
            @Value("${replyts.tenant}") String tenant,
            @Value("${service.discovery.hostname:localhost}") String discoveryHostname,
            @Value("${service.discovery.port:8500}") int discoveryPort,
            @Value("${hazelcast.password}") String hazelcastPassword,
            @Value("${hazelcast.ip}") String hazelcastIp,
            @Value("${hazelcast.port:5701}") int hazelcastPort,
            @Value("${hazelcast.port.increment:false}") boolean hazelcastPortIncrement) {
        Config config = new Config();

        config.getGroupConfig().setName(format("comaas_%s", tenant));
        config.getGroupConfig().setPassword(hazelcastPassword);

        config.getProperties().setProperty("hazelcast.jmx", "true");
        config.getProperties().setProperty("hazelcast.phone.home.enabled", "false");
        config.getProperties().setProperty("hazelcast.logging.type", "slf4j");
        config.getProperties().setProperty("hazelcast.discovery.enabled", "true");
        config.getProperties().setProperty("hazelcast.http.healthcheck.enabled", "true");
        config.getProperties().setProperty("hazelcast.socket.bind.any", "false");

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

        config.getNetworkConfig().setPortAutoIncrement(hazelcastPortIncrement);
        config.getNetworkConfig().setPort(hazelcastPort);

        config.getNetworkConfig().setPublicAddress(hazelcastIp);
        config.getNetworkConfig().getInterfaces().setEnabled(true).addInterface(hazelcastIp);

        Map<String, Comparable> properties = new HashMap<>();
        properties.put("consul-host", discoveryHostname);
        properties.put("consul-port", String.valueOf(discoveryPort));
        properties.put("consul-service-name", format("comaas-core-%s", tenant));
        properties.put("consul-healthy-only", "true");
        properties.put("consul-service-tags", "hazelcast");
        properties.put("consul-discovery-delay-ms", "0");
        properties.put("consul-registrator", DoNothingRegistrator.class.getName());

        DiscoveryStrategyConfig strategy = new DiscoveryStrategyConfig(ConsulDiscoveryStrategy.class.getName(), properties);

        config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(strategy);

        return config;
    }

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance hazelcastInstance(Config config) {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        LOG.info("Hazelcast Cluster Members (name): {}", hazelcastInstance.getConfig().getGroupConfig().getName());
        LOG.info("Hazelcast Cluster Members (actually): {}", hazelcastInstance.getCluster().getMembers());

        return hazelcastInstance;
    }

    @Bean
    public SpringContextProvider mainContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/", new String[] { "classpath:control-context.xml" }, context);
    }

    @Bean
    public SpringContextProvider configContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/configv2", new String[] { "classpath:config-mvc-context.xml" }, context);
    }

    @Bean
    public SpringContextProvider screeningContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/screeningv2", new String[] { "classpath:screening-mvc-context.xml" }, context);
    }

    @Bean
    public Webserver webserver(List<SpringContextProvider> contextProviders) {
        contextProviders.forEach(c -> LOG.info("Registering context {}", c.getPath()));

        return new Webserver(contextProviders);
    }

    public static void main(String[] args) {
        try {
            // XXX: Refactor this into a ApplicationContextInitializer once on Spring Boot

            AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();

            parent.getEnvironment().getPropertySources().addFirst(new ComaasPropertySource());

            parent.register(LoggingService.class, CloudConfiguration.class);

            parent.refresh();

            // XXX: Initialize the application as we were doing before; as going annotation-only requires quite some more refactoring as parent/child is not as easily supported

            AbstractApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
              "classpath:super-mega-temporary-context.xml",
            }, false, parent);

            context.getEnvironment().setActiveProfiles(PRODUCTIVE_PROFILE);

            context.registerShutdownHook();

            context.refresh();

            context.getBean(StartupExperience.class).running(context.getBean(HttpServerFactory.class).getPort());

            context.publishEvent(new ApplicationReadyEvent(context));
        } catch (Exception e) {
            LOG.error("Unable to start Comaas", e);
        }
    }
}
