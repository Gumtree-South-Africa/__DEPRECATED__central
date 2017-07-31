package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.api.processing.MessageFixer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

@Configuration
@ComponentScan(value = { "com.ecg", "com.ebay" }, useDefaultFilters = false, includeFilters = @ComponentScan.Filter(ComaasPlugin.class))
@Import({ StartupExperience.class, EmbeddedWebserver.class })
public class ReplyTS {
    private static final Logger LOG = LoggerFactory.getLogger(ReplyTS.class);

    public static final String PRODUCTIVE_PROFILE = "productive";
    public static final String EMBEDDED_PROFILE = "embedded";
    public static final String MIGRATION_PROFILE = "migration";

    @Autowired(required = false)
    private List<MessageProcessedListener> messageProcessedListeners = emptyList();

    @Autowired(required = false)
    private List<MessageFixer> javaMailMessageFixers = emptyList();

    @PostConstruct
    public void reportThings() {
        LOG.info("With MessageProcessedListeners: {}", messageProcessedListeners.stream()
          .map(listener -> listener.getClass().getCanonicalName())
          .collect(joining(", ")));

        LOG.info("With Mail Fixers: {}", javaMailMessageFixers.stream()
          .map(fixer -> fixer.getClass().getCanonicalName())
          .collect(joining(", ")));
    }

    @Bean
    public Config hazelcastConfiguration(@Value("${confDir}/hazelcast.xml") String location) throws IOException {
        if (location.startsWith("classpath")) {
            InputStream inputStream = new ClassPathResource(location.substring(location.indexOf(":") + 1)).getInputStream();
            return new XmlConfigBuilder(inputStream).build();
        } else {
            return new FileSystemXmlConfig(location);
        }
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
        webserver.context(new SpringContextProvider("/", new String[] { "classpath:" + contextLocation }, context));

        webserver.start();

        return experience.running(webserver.getPort());
    }

    public static void main(String[] args) throws Exception {
        try {
            AbstractApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
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
}