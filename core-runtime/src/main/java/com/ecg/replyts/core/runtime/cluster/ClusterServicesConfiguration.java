package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.ClusterMonitor;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Deprecated
public class ClusterServicesConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterServicesConfiguration.class);

    @Bean
    public HazelcastInstance clusterMember(Config config) {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);

        LOG.info("Hazelcast Cluster Members (name): " + hazelcastInstance.getConfig().getGroupConfig().getName());
        LOG.info("Hazelcast Cluster Members (configured): " + config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());
        LOG.info("Hazelcast Cluster Members (actually): " + hazelcastInstance.getCluster().getMembers());

        return hazelcastInstance;
    }
}