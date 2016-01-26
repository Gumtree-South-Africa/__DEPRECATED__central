package com.ecg.replyts.core.runtime.persistence;


import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.raw.pbc.PBClientConfig;
import com.basho.riak.client.raw.pbc.PBClusterConfig;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.core.runtime.cluster.RiakHostConfig;
import com.ecg.replyts.core.runtime.persistence.conditional.RiakEnabledConditional;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Profile(ReplyTS.PRODUCTIVE_PROFILE)
public class RiakClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RiakClientConfiguration.class);

    @Autowired
    private RiakHostConfig hostConfig;

    @Value("${persistence.riak.enabled:true}")
    private boolean riakEnabled;

    @Value("${persistence.riak.connectionPoolSizePerRiakHost}")
    private int connectionPoolSizePerRiakHost;

    @Value("${persistence.riak.maxConnectionsToRiakCluster}")
    private int totalMaxConnectionsToRiakCluster;

    @Value("${persistence.riak.connectionTimeoutMs}")
    private int connectionTimeoutMs;

    @Value("${persistence.riak.requestTimeoutMs}")
    private int requestTimeoutMs;

    @Value("${persistence.riak.idleConnectionTimeoutMs}")
    private int idleConnectionTtlMs;

    // The Riak cluster client for the primary datacenter
    private IRiakClient primaryRiakClusterClient;

    @PostConstruct
    void setup() throws RiakException {
        if (riakEnabled) {
            LOG.info("Riak Hosts in primary datacenter: {}", hostConfig.getHostList());

            primaryRiakClusterClient = createPrimaryRiakClusterClient();
        }
    }

    private IRiakClient createPrimaryRiakClusterClient() throws RiakException {
        // The server should only connect to the Riak notes in the same (primary) datacenter.
        List<RiakHostConfig.Host> primaryHostList = hostConfig.getHostList();

        // Note: The parameter totalMaximumConnections makes only sense if max connections for the cluster is smaller
        // that the sum of the pool size per node.
        PBClusterConfig config = new PBClusterConfig(totalMaxConnectionsToRiakCluster);
        PBClientConfig clientConfig = createClientConfigBuilderWithDefaults()
                .withPoolSize(connectionPoolSizePerRiakHost)
                .build();

        config.addHosts(clientConfig, primaryHostListAsStringArray());

        return RiakFactory.newClient(config);
    }

    private String[] primaryHostListAsStringArray() {
        return Lists.transform(hostConfig.getHostList(), new Function<RiakHostConfig.Host, String>() {
            @Nullable
            @Override
            public String apply(RiakHostConfig.Host input) {
                return input.getHost();
            }
        }).toArray(new String[0]);
    }

    private PBClientConfig.Builder createClientConfigBuilderWithDefaults() {
        return new PBClientConfig.Builder()
                .withConnectionTimeoutMillis(connectionTimeoutMs)
                .withIdleConnectionTTLMillis(idleConnectionTtlMs)
                .withInitialPoolSize(0) // we really want this to be 0. otherwise the riak client will not be able to initialize if any riak node is unavailable.
                .withRequestTimeoutMillis(requestTimeoutMs)
                .withPort(hostConfig.getProtobufPort());
    }

    /**
     * The primary riak client to use for persistence operations.
     */
    @Bean
    @Conditional(RiakEnabledConditional.class)
    public IRiakClient primaryRiakClient() {
        return primaryRiakClusterClient;
    }

    @PreDestroy
    void shutdown() {
        if (primaryRiakClusterClient != null) primaryRiakClusterClient.shutdown();
    }

}
