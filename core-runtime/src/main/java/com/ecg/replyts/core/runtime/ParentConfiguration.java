package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableMap;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@PropertySource(name = ParentConfiguration.CONF_DIR_PROPERTY_SOURCE, value = "file:${confDir}/replyts.properties")
@Import({ CloudDiscoveryConfiguration.class, ParentConfiguration.LegacyConfiguration.class })
public class ParentConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ParentConfiguration.class);

    static final String CONF_DIR_PROPERTY_SOURCE = "confDirProperties";

    @Autowired
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void addPortToProperties() {
        String comaasHttpPort = System.getenv("COMAAS_HTTP_PORT");

        if (comaasHttpPort != null) {
            environment.getPropertySources().addFirst(new MapPropertySource("Environment-detected properties", ImmutableMap.of("replyts.http.port", comaasHttpPort)));
        }
    }

    @Configuration
    @ConditionalOnExpression("#{'${service.discovery.enabled:false}' == 'false'}")
    public static class LegacyConfiguration {
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
    }
}
