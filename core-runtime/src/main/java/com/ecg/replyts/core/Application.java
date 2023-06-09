package com.ecg.replyts.core;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.runtime.HttpServerFactory;
import com.ecg.replyts.core.runtime.StartupExperience;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.prometheus.PrometheusExporter;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.logstash.logback.argument.StructuredArguments;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategy;
import org.bitsofinfo.hazelcast.discovery.consul.DoNothingRegistrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.Tenants.TENANT;
import static java.lang.String.format;

@Configuration
@ComponentScan({
        "com.ecg.replyts.app",
        "com.ecg.replyts.core.runtime",
        "com.ecg.comaas.kjca.coremod", // KJCA hacks
        /*
        "com.ecg.gumtree", // Gumtree UK hacks
        "com.ecg.comaas.bt.coremode", // Bolt Hacks (there's no typo in the package name)
        "com.ebay.ecg.bolt", // Also Bolt Hack, but different
        "com.ecg.replyts.autogatemaildelivery", // Gumtree Australia hacks
        "com.ecg.replyts.core.runtime.mailparser" // More Gumtree Australia hacks
        */
})
@ComponentScan(value = { "com.ecg", "com.ebay" }, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(ComaasPlugin.class))
@ImportResource("classpath*:/plugin-inf/*.xml")
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String HAZELCAST_GROUP_NAME_ENV_VAR = "HAZELCAST_GROUP_NAME";

    static {
        java.security.Security.setProperty("networkaddress.cache.ttl" , "60");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOG.error("Uncaught exception in thread {}", thread, throwable));

        LoggingService.bootstrap();
    }

    @Bean
    public Config hazelcastConfiguration(
            @Value("${replyts.tenant}") String tenant,
            @Value("${service.discovery.hostname:localhost}") String discoveryHostname,
            @Value("${service.discovery.port:8599}") int discoveryPort,
            @Value("${hazelcast.password}") String hazelcastPassword,
            @Value("${hazelcast.ip:127.0.0.1}") String hazelcastIp,
            @Value("${hazelcast.port:5701}") int hazelcastPort,
            @Value("${hazelcast.port.increment:false}") boolean hazelcastPortIncrement) {
        Config config = new Config();

        if (System.getenv(HAZELCAST_GROUP_NAME_ENV_VAR) != null) {
            config.getGroupConfig().setName(System.getenv(HAZELCAST_GROUP_NAME_ENV_VAR));
        } else {
            config.getGroupConfig().setName(format("comaas_%s", tenant));
        }

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
        LOG.info("Effective hazelcast configuration: {}", config);
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

            parent.register(LoggingService.class, CloudConfiguration.class, PrometheusExporter.class);

            parent.refresh();

            // XXX: Initialize the application as we were doing before; as going annotation-only requires quite some more refactoring as parent/child is not as easily supported

            AbstractRefreshableApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
              "classpath:super-mega-temporary-context.xml",
            }, false, parent);

            context.registerShutdownHook();

            context.getEnvironment().setActiveProfiles(parent.getEnvironment().getProperty(TENANT));

            context.refresh();

            context.getBean(StartupExperience.class).running(context.getBean(HttpServerFactory.class).getPort());

            context.publishEvent(new ApplicationReadyEvent(context));

            logRegisteredComponents(context);
        } catch (Exception e) {
            LOG.error("Unable to start Comaas", e);
        }
    }

    private static void logRegisteredComponents(AbstractRefreshableApplicationContext context) {
        ArrayNode filters = ObjectMapperConfigurer.arrayBuilder();
        context.getBeansOfType(Filter.class).values().stream().forEach(comp -> filters.add(comp.getClass().getName()));

        ArrayNode preProcessors = ObjectMapperConfigurer.arrayBuilder();
        context.getBeansOfType(PreProcessor.class).values().stream().forEach(comp -> preProcessors.add(comp.getClass().getName()));

        ArrayNode postProcessors = ObjectMapperConfigurer.arrayBuilder();
        context.getBeansOfType(PostProcessor.class).values().stream().forEach(comp -> postProcessors.add(comp.getClass().getName()));

        ArrayNode listeners = ObjectMapperConfigurer.arrayBuilder();
        context.getBeansOfType(MessageProcessedListener.class).values().stream().forEach(comp -> listeners.add(comp.getClass().getName()));

        ObjectNode components = ObjectMapperConfigurer.objectBuilder();
        components.set("pre_processors", preProcessors);
        components.set("filters", filters);
        components.set("post_processors", postProcessors);
        components.set("listeners", listeners);

        LOG.info("Registration Completed", StructuredArguments.raw("components", components.toString()));
    }
}
