package com.ecg.replyts.core.runtime.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@Deprecated // This was only triggered by a Riak outage (Riak Sanity check) which is deprecated as well
@Component
@ConditionalOnExpression(
  "'${riak.cluster.monitor.enabled:true}' == 'true' && " +
  "('${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid'))"
)
public class ClusterModeControl implements ClusterModeControlMBean {
    @Autowired
    private ClusterModeManager manager;

    @Value("${cluster.jmx.enabled:true}")
    private Boolean isJmxEnabled;

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
