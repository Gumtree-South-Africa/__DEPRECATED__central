package com.ecg.replyts.core.runtime.cluster.monitor;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.NodeStats;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class RiakClientHealthWrapperTest {

    @Mock
    private IRiakClient client;

    @Mock
    private NodeStats stats;

    private RiakClientHealthWrapper check;

    @Before
    public void setUp() {
        check = new RiakClientHealthWrapper(client);
        when(stats.nodename()).thenReturn("node3");
    }

    @Test
    public void moreThan50PercentAvailableMeansHealthy() throws RiakException {

        when(stats.connectedNodes()).thenReturn(new String[]{"node1", "node2"});   // the requesting node will be counted as ok
        when(stats.ringMembers()).thenReturn(new String[]{"node1", "node2", "node3", "node4"});

        when(client.stats()).thenReturn(singletonList(stats));

        CheckResult result = check.clusterHealthStat();

        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getHealthyNodes()).containsOnly("node1", "node2", "node3");
        assertThat(result.getImpairedNodes()).containsOnly("node4");
    }

    @Test
    public void lessThan50PercentAvailableMeansUnhealthy() throws RiakException {

        when(stats.connectedNodes()).thenReturn(empty());
        when(stats.ringMembers()).thenReturn(new String[]{"node1", "node2", "node3", "node4"});

        when(client.stats()).thenReturn(singletonList(stats));

        CheckResult result = check.clusterHealthStat();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getHealthyNodes()).containsOnly("node3");
        assertThat(result.getImpairedNodes()).containsOnly("node1", "node2", "node4");
    }

    @Test
    public void fiftyPercentMeansUnhealthy() throws RiakException {

        when(stats.connectedNodes()).thenReturn(new String[]{"node1"});
        when(stats.ringMembers()).thenReturn(new String[]{"node1", "node2", "node3", "node4"});

        when(client.stats()).thenReturn(singletonList(stats));

        CheckResult result = check.clusterHealthStat();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getHealthyNodes()).containsOnly("node1", "node3");
        assertThat(result.getImpairedNodes()).containsOnly("node2", "node4");
    }

    @Test
    public void shouldNotHealthyIfNoClusterMember() throws RiakException {

        when(stats.connectedNodes()).thenReturn(empty());
        when(stats.ringMembers()).thenReturn(empty());
        when(stats.nodename()).thenReturn("node1");

        when(client.stats()).thenReturn(singletonList(stats));

        CheckResult result = check.clusterHealthStat();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getHealthyNodes()).isEmpty();
        assertThat(result.getImpairedNodes()).isEmpty();
    }

    /**
     * Check for single node cluster. This might happen when an node was remove from the cluster and later started up
     * without putting back to cluster. Data lost might happen.
     */
    @Test
    public void shouldNotHealthyIfNoSingleNodeCluster() throws RiakException {

        when(stats.connectedNodes()).thenReturn(empty());
        when(stats.ringMembers()).thenReturn(new String[]{"node1"});
        when(stats.nodename()).thenReturn("node1");

        when(client.stats()).thenReturn(singletonList(stats));

        CheckResult result = check.clusterHealthStat();

        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getHealthyNodes()).isEmpty();
        assertThat(result.getImpairedNodes()).isEmpty();
    }

    private String[] empty() {
        return new String[]{};
    }

}