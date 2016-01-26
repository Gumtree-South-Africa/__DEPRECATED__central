package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.ClusterMonitor;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;


/**
 * Creates Beans for Cluster-Communication related services
 */
public class ClusterServicesConfiguration {



    private static final Logger LOG = LoggerFactory.getLogger(ClusterServicesConfiguration.class);

    @Bean
    public HazelcastInstance hazelcast(Config config) {

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        LOG.info("Hazelcast Cluster Members (configured): "+config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());
        LOG.info("Hazelcast Cluster Members (actually): "+hazelcastInstance.getCluster().getMembers());
        return hazelcastInstance;
    }


    @Bean
    public Guids guids() {
        return new Guids();
    }

    @Bean
    public ClusterModeManager clusterModeManager(ClusterMonitor clusterMonitor) {
        return new ClusterModeManager(clusterMonitor);
    }

    @Bean
    public ClusterModeControl clusterModeControl(ClusterModeManager modeManager) {
        return new ClusterModeControl(modeManager);
    }
}
