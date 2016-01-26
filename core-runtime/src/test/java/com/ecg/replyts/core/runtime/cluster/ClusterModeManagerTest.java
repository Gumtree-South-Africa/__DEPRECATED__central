package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.ClusterMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterModeManagerTest {

    @Mock
    private ClusterMonitor detector;

    private ClusterModeManager manager;

    @Before
    public void setup() {
        manager = new ClusterModeManager(detector);
    }

    @Test
    public void passOkMode() {
        when(detector.allDatacentersAvailable()).thenReturn(true);
        assertThat(manager.determineMode(), is(ClusterMode.OK));
    }

    @Test
    public void switchToBlocked() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        assertThat(manager.determineMode(), is(ClusterMode.BLOCKED));
    }

    @Test
    public void switchToFromBlockedToFailover() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        manager.determineMode();
        assertThat(manager.determineMode(), is(ClusterMode.BLOCKED));
        manager.switchToFailover();
        assertThat(manager.determineMode(), is(ClusterMode.FAILOVER));
    }

    @Test
    public void stickToBlockedAfterRecovery() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        manager.determineMode();
        assertThat(manager.determineMode(), is(ClusterMode.BLOCKED));
        when(detector.allDatacentersAvailable()).thenReturn(true);
        assertThat(manager.determineMode(), is(ClusterMode.BLOCKED));
    }

    @Test
    public void switchFromFailoverBackToBlockedOnRecovery() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        manager.determineMode();
        manager.switchToFailover();
        assertThat(manager.determineMode(), is(ClusterMode.FAILOVER));
        when(detector.allDatacentersAvailable()).thenReturn(true);
        assertThat(manager.determineMode(), is(ClusterMode.BLOCKED));
    }

    @Test
    public void switchFromBlockedToNormalAfterRecovery() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        manager.determineMode();
        when(detector.allDatacentersAvailable()).thenReturn(true);
        manager.switchToNormal();
        assertThat(manager.determineMode(), is(ClusterMode.OK));
    }


    @Test(expected = IllegalStateException.class)
    public void rejectSwitchToNormalIfClusterNotHealthy() {
        when(detector.allDatacentersAvailable()).thenReturn(false);
        manager.determineMode();
        manager.switchToNormal();
    }

}
