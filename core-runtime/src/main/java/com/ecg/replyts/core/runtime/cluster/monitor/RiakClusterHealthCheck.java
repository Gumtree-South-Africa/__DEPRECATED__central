package com.ecg.replyts.core.runtime.cluster.monitor;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.ecg.replyts.core.runtime.cluster.RiakHostConfig.Host;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * Checks the Riak cluster health. This class takes care about the exception handling and delegate.
 * <p/>
 * Algorithm:
 * Run through all configured primary nodes. If on one node less that 50% of the cluster members are connected,
 * unhealthy status will be reported.
 * Reason to check all nodes: If one node will be removed in a controlled manner and the node will be restarted without
 * brings it back to the cluster, the node acts like a single-node cluster which means inconsistency on persistence.
 * Therefore all nodes will be checked if ok.
 * <p/>
 * If one node not reachable, the node will be skipped, because of fault tolerance its ok. Only if all nodes are not reachable,
 * unhealthy will be reported.
 */
class RiakClusterHealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakClusterHealthCheck.class);
    private static final int MAX_ATTEMPTS = 3;

    private final Map<Host, RiakClientHealthWrapper> clientWrappers;

    // need a factory method here because constructors would have same signature after generic erasure
    public static RiakClusterHealthCheck createFromRiakClients(Map<Host, IRiakClient> clients) {
        return new RiakClusterHealthCheck(wrap(clients));
    }

    // for tests
    RiakClusterHealthCheck(Map<Host, RiakClientHealthWrapper> clientWrappers) {
        this.clientWrappers = clientWrappers;
    }

    private static Map<Host, RiakClientHealthWrapper> wrap(Map<Host, IRiakClient> clients) {
        ImmutableMap.Builder<Host, RiakClientHealthWrapper> builder = ImmutableMap.builder();
        for (Map.Entry<Host, IRiakClient> entry : clients.entrySet()) {
            builder.put(entry.getKey(), new RiakClientHealthWrapper(entry.getValue()));
        }
        return builder.build();
    }

    /**
     * @return The result, never {@code null}.
     */
    public CheckResult check() {

        boolean checkNextNode = true;
        CheckResult result = CheckResult.createNonHealthyEmpty();
        Iterator<Map.Entry<Host, RiakClientHealthWrapper>> iterator = clientWrappers.entrySet().iterator();

        // On all configured nodes trigger a stats call and see if all nodes see more than 50% connected nodes
        // Run through all configured nodes to detect single-node-cluster (see doc in check class)
        while (iterator.hasNext() && checkNextNode) {
            Map.Entry<Host, RiakClientHealthWrapper> entry = iterator.next();

            result = checkNode(entry.getValue(), entry.getKey());
            // Check the next node if this node is healthy. Check until one node is unhealthy to find single-cluster-node problems.
            checkNextNode = result.isHealthy();
        }
        LOGGER.debug(result.toString());
        // if healthy: the last healthy result, else the failed
        return result;
    }

    /**
     * Make getting the stats more robust. Starting with Riak 2.x on server side we had some 500er responses.
     * In combination with a load balancer and only one node configured the check goes immediately in error mode.
     * Therefore do a retry on exception.
     */
    private CheckResult checkNode(RiakClientHealthWrapper node, Host host) {

        int attempts = 1;
        while (attempts < MAX_ATTEMPTS + 1) {
            try {
                CheckResult result = node.clusterHealthStat();
                LOGGER.debug(result.toString());
                return result;
            } catch (RiakException e) {
                LOGGER.warn("Error on requesting stats from Riak node {} on attempt {}/{}", host, attempts, MAX_ATTEMPTS, e);
                attempts++;
            }
        }
        LOGGER.error("Requesting stats from Riak node {} failed!", host);

        return CheckResult.createNonHealthyEmpty();
    }

}
