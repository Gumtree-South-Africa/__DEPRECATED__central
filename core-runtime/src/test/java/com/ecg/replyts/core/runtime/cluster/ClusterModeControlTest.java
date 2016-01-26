package com.ecg.replyts.core.runtime.cluster;

import org.junit.Test;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author mhuttar
 */
public class ClusterModeControlTest {

    private ClusterModeManager control = mock(ClusterModeManager.class);

    @Test
    public void registersAsMBean() throws Exception {
        ClusterModeControl clusterModeControl = new ClusterModeControl(control);
        clusterModeControl.start();
        ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName("ReplyTS:type=ClusterControl,name=ClusterModeControl"));
        clusterModeControl.stop();
    }

    @Test
    public void unregistersMbean() throws Exception {
        ClusterModeControl clusterModeControl = new ClusterModeControl(control);
        clusterModeControl.start();
        ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName("ReplyTS:type=ClusterControl,name=ClusterModeControl"));
        clusterModeControl.stop();

        try {
            ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName("ReplyTS:type=ClusterControl,name=ClusterModeControl"));
            fail("MBean should not be available after stop");
        } catch (InstanceNotFoundException e) {
            // this is okay!
        }

    }
}
