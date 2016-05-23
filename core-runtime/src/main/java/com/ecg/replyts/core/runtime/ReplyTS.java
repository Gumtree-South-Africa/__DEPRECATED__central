package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
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
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@PropertySource("discovery.properties")
//@EnableDiscoveryClient
//@EnableAutoConfiguration
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

    @Configuration
    @Profile(PRODUCTIVE_PROFILE)
    @PropertySource("file:${confDir}/replyts.properties")
    static class ProductionProperties { }

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
