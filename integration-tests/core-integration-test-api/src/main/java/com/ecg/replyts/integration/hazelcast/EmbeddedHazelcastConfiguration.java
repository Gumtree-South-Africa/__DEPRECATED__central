package com.ecg.replyts.integration.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedHazelcastConfiguration {
    private static final String HAZELCAST_GROUP_NAME_ENV_VAR = "HAZELCAST_GROUP_NAME";

    @Bean
    public Config hazelcastConfiguration() {
        Config config = new Config();

        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

        if (System.getenv(HAZELCAST_GROUP_NAME_ENV_VAR) != null) {
            config.setGroupConfig(new GroupConfig(System.getenv(HAZELCAST_GROUP_NAME_ENV_VAR), System.getenv(HAZELCAST_GROUP_NAME_ENV_VAR)));
        }

        return config;
    }
}
