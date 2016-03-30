package com.ecg.replyts.core.runtime.cluster;

import org.springframework.beans.factory.annotation.Value;

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

    @Value("${cluster.jmx.enabled:true}")
    private Boolean isJmxEnabled;

    ClusterModeControl(ClusterModeManager manager) {
        this.manager = manager;
    }

    @PostConstruct
    void start() {
        if (isJmxEnabled != null && !isJmxEnabled)
            return;

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            ObjectName objectName = buildObjectName();

            if (!server.isRegistered(objectName))
                server.registerMBean(this, objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Can not register ClusterModeControl", e);
        }
    }

    @PreDestroy
    void stop() {
        if (isJmxEnabled != null && !isJmxEnabled)
            return;

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            ObjectName objectName = buildObjectName();

            if (server.isRegistered(objectName))
                server.unregisterMBean(objectName);
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
