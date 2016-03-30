package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Exposes all jobs as MBeans.
 *
 * @author mhuttar
 */
class JmxInvokeSupport {
    private final CronJobService srvc;
    private final List<CronJobExecutor> executors;

    private static final Logger LOG = LoggerFactory.getLogger(JmxInvokeSupport.class);

    public JmxInvokeSupport(CronJobService srvc, List<CronJobExecutor> executors) {
        this.srvc = srvc;
        this.executors = executors;

        for (CronJobExecutor e : executors) {
            register(e);
        }
    }

    /**
     * registers an MBean for a Cron executor
     *
     * @param cje executor to register mbean for
     */
    private void register(CronJobExecutor cje) {
        try {
            ObjectName invokerObjectName = toOName(cje);
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            if (mbeanServer.isRegistered(invokerObjectName)) {
                LOG.warn("Cannot Expose MBean for Job {}: It is already registered", cje.getClass().getName());
            } else {
                LOG.debug("Registering Cron Job Launcher MBean: {}", invokerObjectName.toString());
                mbeanServer.registerMBean(new CronJobInvoker(cje, srvc), invokerObjectName);
            }
        } catch (Exception ex) {
            LOG.error("Could not register Cron Job invoker MBean for " + cje.getClass(), ex);
        }
    }

    /**
     * unregisters all mbeans
     */
    void stop() {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        for (CronJobExecutor cje : executors) {
            try {
                ObjectName objectName = toOName(cje);

                if (mbeanServer.isRegistered(objectName))
                    mbeanServer.unregisterMBean(objectName);
            } catch (Exception e) {
                LOG.error("Could not unregister MBean for " + cje.getClass(), e);
            }
        }
    }

    private ObjectName toOName(CronJobExecutor cje) throws MalformedObjectNameException {
        String objectName = String.format("ReplyTS:type=CronJobs,name=%s", cje.getClass().getSimpleName());
        return new ObjectName(objectName);
    }
}
