package com.ecg.replyts.integration.hazelcast;

import com.hazelcast.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.String.format;

@Configuration
public class EmbeddedHazelcastConfiguration {
    @Bean
    public Config hazelcastConfiguration(
            @Value("${tenant}") String tenant,
            @Value("${service.discovery.hostname:localhost}") String discoveryHostname,
            @Value("${service.discovery.port:8500}") int discoveryPort,
            @Value("${hazelcast.password}") String hazelcastPassword,
            @Value("${hazelcast.ip:#{null}}") String hazelcastIp,
            @Value("${hazelcast.port:5701}") int hazelcastPort,
            @Value("${hazelcast.port.increment:false}") boolean hazelcastPortIncrement) {

        Config config = new Config();

        config.getGroupConfig().setName(format("comaas_%s", tenant));
        config.getGroupConfig().setPassword(hazelcastPassword);

        config.getProperties().setProperty("hazelcast.jmx", "true");
        config.getProperties().setProperty("hazelcast.phone.home.enabled", "false");
        config.getProperties().setProperty("hazelcast.logging.type", "slf4j");
        config.getProperties().setProperty("hazelcast.discovery.enabled", "true");

        return config;
    }

}
