package com.ecg.replyts.core.runtime.cluster.monitor;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.raw.http.HTTPClientConfig;
import com.ecg.replyts.core.api.ClusterMonitor;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig.Host;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Profile(ReplyTS.PRODUCTIVE_PROFILE)
@Configuration
class ClusterMonitorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterMonitorConfiguration.class);

    private static final String RIAK_CLUSTER_SANITY_CHECK = "riakClusterSanityCheck";

    @Autowired
    private RiakHostConfig hostConfig;

    @Value("${riak.cluster.monitor.check.attempts:3}")
    private int attemptsBeforeFailure;

    @Value("${riak.cluster.monitor.check.intervalSeconds:2}")
    private int checkIntervalSeconds;

    @Value("${riak.cluster.monitor.http.client.timeout.ms:10000}")
    private int httpClientTimeoutMs;

    @Value("${riak.cluster.monitor.enabled:true}")
    private boolean checkEnabled;

    @Bean
    public ClusterMonitor clusterMonitor(@Qualifier(RIAK_CLUSTER_SANITY_CHECK) Check riakClusterSanityCheck) {
        long checkIntervalMillis = TimeUnit.SECONDS.toMillis(checkIntervalSeconds);
        return new RiakClusterMonitor(checkIntervalMillis, riakClusterSanityCheck);
    }

    private static Map<Host, IRiakClient> createHttpRiakClients(RiakHostConfig riakHostConfig, int httpClientTimeoutMs) throws RiakException {
        ImmutableMap.Builder<Host, IRiakClient> clients = ImmutableMap.builder();
        List<Host> hostList = riakHostConfig.getHostList();

        for (Host host : hostList) {
            HTTPClientConfig config = new HTTPClientConfig.Builder()
                    .withHost(host.getHost())
                    .withPort(riakHostConfig.getHttpPort())
                    .withTimeout(httpClientTimeoutMs)
                    .build();
            clients.put(host, RiakFactory.newClient(config));
        }
        return clients.build();
    }

    @Bean(name = RIAK_CLUSTER_SANITY_CHECK)
    public Check riakClusterSanityCheck() throws RiakException {
        return checkEnabled ? createRiakClusterSanityCheck() : createAlwaysHealthyCheck();
    }

    private AlwaysHealthyCheck createAlwaysHealthyCheck() {
        LOG.info("DISABLED Riak cluster check");
        return new AlwaysHealthyCheck();
    }

    private RiakClusterSanityCheck createRiakClusterSanityCheck() throws RiakException {
        LOG.info("Riak cluster check enabled.");
        RiakClusterHealthCheck riakClusterHealthCheck = RiakClusterHealthCheck.createFromRiakClients(createHttpRiakClients(hostConfig, httpClientTimeoutMs));
        return new RiakClusterSanityCheck(riakClusterHealthCheck, attemptsBeforeFailure);
    }

    @Bean
    public RiakClusterCheckProvider riakClusterCheckProvider(
            ClusterModeManager clusterModeManager,
            @Qualifier(RIAK_CLUSTER_SANITY_CHECK) Check sanityCheck
    ) throws RiakException {
        return new RiakClusterCheckProvider(clusterModeManager, sanityCheck);
    }

}
