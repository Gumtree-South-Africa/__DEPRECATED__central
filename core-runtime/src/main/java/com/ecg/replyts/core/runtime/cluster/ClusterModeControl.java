package com.ecg.replyts.core.runtime.cluster;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author mhuttar
 */
class ClusterModeControl implements ClusterModeControlMBean {

    private final ClusterModeManager manager;

    ClusterModeControl(ClusterModeManager manager) {
        this.manager = manager;
    }

    @PostConstruct
    void start() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.registerMBean(this, buildObjectName());
        } catch (Exception e) {
            throw new IllegalStateException("Can not register ClusterModeControl", e);
        }
    }

    @PreDestroy
    void stop() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.unregisterMBean(buildObjectName());
        } catch (Exception e) {
            throw new IllegalStateException("Can not register ClusterModeControl", e);
        }
    }

    @Override
    public void switchToFailoverMode() {
        manager.switchToFailover();
    }

    @Override
    public void switchToNormalMode() {
        manager.switchToNormal();
    }

    private ObjectName buildObjectName() {
        try {
            String on = "ReplyTS:type=ClusterControl,name=ClusterModeControl";
            return new ObjectName(on);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }

    }

}
