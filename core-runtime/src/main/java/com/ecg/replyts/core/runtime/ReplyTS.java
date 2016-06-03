package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.hazelcast.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Import({ StartupExperience.class, EmbeddedWebserver.class })
public class ReplyTS {
    private static final Logger LOG = LoggerFactory.getLogger(ReplyTS.class);

    /**
     * Spring profile which instantiates externally dependent beans
     */
    public static final String PRODUCTIVE_PROFILE = "productive";

    /**
     * Spring profile which instantiates embedded/test beans.
     */
    public static final String EMBEDDED_PROFILE = "embedded";

    /**
     * Load replyts.properties from ${confDir} as well as auto-discovery (if enabled).
     */
    @Configuration
    @Profile(PRODUCTIVE_PROFILE)
    @EnableDiscoveryClient
    @EnableAutoConfiguration
    @PropertySource("file:${confDir}/replyts.properties")
    @PropertySource("discovery.properties")
    static class ProductionProperties {
        private static final String INSTANCE_CASSANDRA = "cassandra";
        private static final String PROPERTY_CASSANDRA_ENDPOINT = "persistence.cassandra.endpoint";

        @Value("#{'${persistence.riak.enabled:false}' ? 'riak' : 'cassandra'}")
        private String conversationRepositorySource;

        @Autowired
        private ConfigurableEnvironment environment;

        @Autowired(required = false)
        private DiscoveryClient discoveryClient;

        @PostConstruct
        @ConditionalOnBean(DiscoveryClient.class)
        private void autoDiscoverySelf() {
            LOG.info("Registered service under instance {}", discoveryClient.getLocalServiceInstance().getUri().toString());
        }

        @PostConstruct
        @ConditionalOnBean(DiscoveryClient.class)
        private void autoDiscoveryOverrides() {
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
                    LOG.info("Unable to auto-discover instances to populate property {} with ({} found)", PROPERTY_CASSANDRA_ENDPOINT, instances.size());
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", gatheredProperties));
        }
    }

    @Bean
    public Config hazelcastConfiguration(@Value("${confDir}/hazelcast.xml") String location) throws IOException {
        if (location.startsWith("classpath")) {
            InputStream inputStream = new ClassPathResource(location.substring(location.indexOf(":") + 1)).getInputStream();

            return new XmlConfigBuilder(inputStream).build();
        } else
            return new FileSystemXmlConfig(location);
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
            }, false);

            context.getEnvironment().setActiveProfiles(PRODUCTIVE_PROFILE);

            context.registerShutdownHook();

            context.refresh();
        } catch (Exception e) {
            LOG.error("COMaaS Abnormal Shutdown", e);

            throw e;
        }
    }
}
