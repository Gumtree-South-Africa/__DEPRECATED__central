package com.ecg.replyts.core.runtime.cluster.monitor;

import com.basho.riak.client.RiakException;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakClusterHealthCheckTest {

    @Mock
    private RiakClientHealthWrapper node1;

    @Mock
    private RiakClientHealthWrapper node2;

    @Mock
    private RiakHostConfig.Host host1;

    @Mock
    private RiakHostConfig.Host host2;

    private RiakClusterHealthCheck check;

    @Before
    public void setUp() {
        check = new RiakClusterHealthCheck(ImmutableMap.of(host1, node1, host2, node2));
    }

    private static CheckResult healthy() {
        return CheckResult.createHealthyEmpty();
    }

    private static CheckResult unhealthy() {
        return CheckResult.createNonHealthyEmpty();
    }

    @Test
    public void shouldHealthyIfAllNodesHealty() throws RiakException {
        when(node1.clusterHealthStat()).thenReturn(healthy());
        when(node2.clusterHealthStat()).thenReturn(healthy());

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    public void shouldUnhealthyIfHalfOfTheNodesUnhealthy() throws RiakException {
        when(node1.clusterHealthStat()).thenReturn(healthy());
        when(node2.clusterHealthStat()).thenReturn(unhealthy());

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    public void shouldUnhealthyIfAllOfTheNodesUnhealthy() throws RiakException {
        when(node1.clusterHealthStat()).thenReturn(unhealthy());
        when(node2.clusterHealthStat()).thenReturn(unhealthy());

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    public void shouldHealthyIfOnNodeNotReachable() throws RiakException {
        when(node1.clusterHealthStat())
                .thenThrow(RiakException.class)
                .thenReturn(healthy());
        when(node2.clusterHealthStat()).thenReturn(healthy());

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    public void shouldUnhealthyIfAllNodesNotReachable() throws RiakException {
        when(node1.clusterHealthStat()).thenThrow(RiakException.class);
        when(node2.clusterHealthStat()).thenThrow(RiakException.class);

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    public void shouldHealthyWorkOnRetry() throws RiakException {
        when(node1.clusterHealthStat()).thenThrow(RiakException.class);
        when(node2.clusterHealthStat()).thenReturn(healthy());

        CheckResult result = check.check();

        assertThat(result.isHealthy()).isFalse();
    }

}
