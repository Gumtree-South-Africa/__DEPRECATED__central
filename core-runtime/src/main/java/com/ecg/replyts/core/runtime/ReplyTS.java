package com.ecg.replyts.core.runtime;

import ch.qos.logback.classic.LoggerContext;
import com.ecg.replyts.core.api.processing.MessageFixer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategy;
import org.bitsofinfo.hazelcast.discovery.consul.DoNothingRegistrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.APPLICATION;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.REVISION;
import static java.lang.String.format;

@Configuration
@ComponentScan(value = { "com.ecg", "com.ebay" }, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(ComaasPlugin.class))
@Import({ StartupExperience.class, EmbeddedWebserver.class })
public class ReplyTS {
    private static final Logger LOG = LoggerFactory.getLogger(ReplyTS.class);

    public static final String PRODUCTIVE_PROFILE = "productive";
    public static final String EMBEDDED_PROFILE = "embedded";
    public static final String MIGRATION_PROFILE = "migration";

    @Bean
    public Config hazelcastConfiguration(
      @Value("${replyts.tenant}") String tenant,
      @Value("${service.discovery.enabled:true}") boolean discoveryEnabled,
      @Value("${service.discovery.hostname:localhost}") String discoveryHostname,
      @Value("${service.discovery.port:8500}") int discoveryPort,
      @Value("${hazelcast.password}") String hazelcastPassword,
      @Value("${hazelcast.port:5701}") int hazelcastPort,
      @Value("${hazelcast.port.increment:false}") boolean hazelcastPortIncrement,
      @Value("${hazelcast.members:}") String hazelcastMembers
    ) throws IOException {
        Config config = new Config();

        config.getGroupConfig().setName(format("replyts_%s", tenant));
        config.getGroupConfig().setPassword(hazelcastPassword);

        config.getProperties().setProperty("hazelcast.jmx", "true");
        config.getProperties().setProperty("hazelcast.version.check.enabled", "false");

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);

        config.getNetworkConfig().setPortAutoIncrement(hazelcastPortIncrement);
        config.getNetworkConfig().setPort(hazelcastPort);

        if (discoveryEnabled) {
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

            Map<String, Comparable> properties = new HashMap<>();

            properties.put("consul-host", discoveryHostname);
            properties.put("consul-port", String.valueOf(discoveryPort));
            properties.put("consul-service-name", "comaas-hazelcast");
            properties.put("consul-healthy-only", "true");
            properties.put("consul-service-tags", format("hazelcast, %s", tenant));
            properties.put("consul-discovery-delay-ms", "10000");

            properties.put("consul-registrator", DoNothingRegistrator.class.getName());

            DiscoveryStrategyConfig strategy = new DiscoveryStrategyConfig(ConsulDiscoveryStrategy.class.getName(), properties);

            config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(strategy);
        } else {
            if (!"".equals(hazelcastMembers)) {
                config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
                config.getNetworkConfig().getJoin().getTcpIpConfig().setMembers(Arrays.asList(hazelcastMembers.split(",")).stream().map(String::trim).collect(Collectors.toList()));
            } else {
                config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
            }
        }

        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        LOG.info("Hazelcast Cluster Members (name): " + hazelcastInstance.getConfig().getGroupConfig().getName());
        LOG.info("Hazelcast Cluster Members (configured): " + config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());
        LOG.info("Hazelcast Cluster Members (actually): " + hazelcastInstance.getCluster().getMembers());

        return hazelcastInstance;
    }

    // Need to wait for all ContextProviders to register themselves - so make this a non-lazy @Bean

    @Bean
    @Lazy(false)
    @DependsOn("defaultContextsInitialized")
    public Boolean started(@Value("${replyts.control.context:control-context.xml}") String contextLocation, EmbeddedWebserver webserver, StartupExperience experience, ApplicationContext context) {
        webserver.context(new SpringContextProvider("/", new String[]{"classpath:" + contextLocation}, context));

        webserver.start();

        return experience.running(webserver.getPort());
    }

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setName("main");

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        // Reset and redirect java.util.Logging to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        setLoggerContextProperties();

        try {
            AbstractApplicationContext context = new ClassPathXmlApplicationContext(new String[]{
                    "classpath:server-context.xml",
                    "classpath:runtime-context.xml",
                    "classpath*:/plugin-inf/*.xml",
            }, false, new AnnotationConfigApplicationContext(ParentConfiguration.class));

            context.getEnvironment().setActiveProfiles(PRODUCTIVE_PROFILE);

            context.registerShutdownHook();

            context.refresh();
        } catch (Exception e) {
            LOG.error("COMaaS Abnormal Shutdown", e);

            throw e;
        }
    }

    private static void setLoggerContextProperties() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.putProperty(APPLICATION, ReplyTS.class.getPackage().getImplementationTitle());
        loggerContext.putProperty(REVISION, ReplyTS.class.getPackage().getImplementationVersion());
    }
}