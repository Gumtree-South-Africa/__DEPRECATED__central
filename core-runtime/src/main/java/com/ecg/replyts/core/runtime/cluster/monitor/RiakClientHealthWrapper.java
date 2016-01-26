package com.ecg.replyts.core.runtime.cluster.monitor;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.NodeStats;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static java.lang.String.format;

/**
 * Wrap a Riak client to provide a health check for the cluster the single node see.
 */
class RiakClientHealthWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakClientHealthWrapper.class);

    private final IRiakClient client;

    /**
     * @param client The RiakClient. We expect the simple, non clustered http client because we don't iterate over the stats.
     */
    RiakClientHealthWrapper(IRiakClient client) {
        this.client = client;
    }

    /**
     * The node will be marked a healthy if >50% of the cluster member are connected.
     *
     * @return The health status for a wrapped Riak client.
     * @throws RiakException If requesting of the node fails.
     */
    public CheckResult clusterHealthStat() throws RiakException {
        Iterable<NodeStats> stats = client.stats();
        Iterator<NodeStats> statsIterator = stats.iterator();
        // Expect exactly one item here.
        // Several items can only appear if cluster client with more than one node configured.
        // We use simple single http client connection, therefore only one node here.
        NodeStats nodeStats = statsIterator.next();

        Set<String> connectedNodes = ImmutableSet.copyOf(nodeStats.connectedNodes());
        Set<String> ringMembers = ImmutableSet.copyOf(nodeStats.ringMembers());
        String queriedNode = nodeStats.nodename();

        LOGGER.debug(format("Stats for node %s: connected nodes: %s, ring members: %s", queriedNode, connectedNodes, ringMembers));

        // Check for single node cluster. This might happen when a node was remove from the cluster and later started up again
        // but without putting back to cluster. The node locks like a single-node-cluster. Data written to this node are lost.
        //
        boolean isSingleNodeCluster = ringMembers.size() == 1;
        if (isSingleNodeCluster) {
            return CheckResult.createNonHealthyEmpty();
        }

        // For the unlikely case no ring member, prevent division by zero
        if (ringMembers.isEmpty()) {
            return CheckResult.createNonHealthyEmpty();
        }

        Set<String> healthyNodes = new HashSet<>(connectedNodes);
        healthyNodes.add(queriedNode);

        Set<String> impairedNodes = new HashSet<>(ringMembers);
        impairedNodes.removeAll(healthyNodes);

        boolean healthy = (double) healthyNodes.size() / (double) ringMembers.size() > 0.5;

        return new CheckResult(healthy, healthyNodes, impairedNodes);
    }

}
