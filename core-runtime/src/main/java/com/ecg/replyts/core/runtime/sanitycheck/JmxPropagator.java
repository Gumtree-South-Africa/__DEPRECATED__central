package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.runtime.sanitycheck.adapter.CheckAdapter;
import com.ecg.replyts.core.runtime.sanitycheck.adapter.CheckAdapterAggregator;
import com.ecg.replyts.core.runtime.sanitycheck.adapter.SingleCheckAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Register all checks from {@link CheckAdapterAggregator} at the MBean server.<br>
 *
 * @author smoczarski
 */
public class JmxPropagator {

    private static final Logger LOG = LoggerFactory.getLogger(JmxPropagator.class);

    private final Map<Check, CheckAdapter> checks = new HashMap<>();
    private final StaticApplicationNamingStrategy namingStrategy;
    private final CheckAdapterAggregator checksAggregator = new CheckAdapterAggregator();
    private final IntervallRefresher refresher;

    private volatile boolean started;

    public JmxPropagator(String applicationName) {
        this.namingStrategy = new StaticApplicationNamingStrategy(applicationName);
        this.refresher = new IntervallRefresher(checksAggregator);
    }

    public synchronized void addCheck(List<Check> checks) {
        for (Check check : checks) {
            addCheck(check);
        }
    }

    private synchronized void addCheck(Check check) {
        if (check == null) {
            throw new IllegalArgumentException("Null not allowed!");
        }
        SingleCheckAdapter adapter = new SingleCheckAdapter(check);

        checks.put(check, adapter);
        checksAggregator.addCheckAdapter(adapter);
        if (started) {
            try {
                registerAdapter(adapter);
            } catch (JMException e) {
                LOG.error("registration of Sanity Check failed: {}", namingStrategy.buildJMXName(adapter), e);
            }
        }
    }


    /**
     * Register all checks of the Sanity Checker at the MBean server. Call this method with the "init-method" hook in
     * Spring.
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;


        try {
            registerAdapter(checksAggregator);

            for (CheckAdapter checkAdapter : checks.values()) {
                try {
                    registerAdapter(checkAdapter);
                } catch (JMException e) {
                    String errMsg = String.format("Could not register adapter with [name=%s, category=%s, subcategory=%s]",
                            checkAdapter.getName(), checkAdapter.getCategory(), checkAdapter.getSubCategory());
                    LOG.error(errMsg, e);
                }
            }
            checksAggregator.execute();
            refresher.start();
        } catch (JMException e) {
            LOG.warn("Registration of Sanity Checks on JMX server failed", e);
        }
    }


    /**
     * Unregister all checks of the Sanity Checker at the MBean server. Call this method with the "destroy-method" hook
     * in Spring.
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        try {
            refresher.stop();
            try {
                for (CheckAdapter checkAdapter : checks.values()) {
                    unregisterAdapter(checkAdapter);
                }
                unregisterAdapter(checksAggregator);
            } catch (JMException e) {
                LOG.warn("Error during unregistering MBean!", e);
            }
            checksAggregator.destroy();
        } finally {
            started = false;
        }
    }

    private void registerAdapter(CheckAdapter checkAdapter) throws JMException {
        ObjectName oname = new ObjectName(namingStrategy.buildJMXName(checkAdapter));
        checkAdapter.setObjectName(oname);
        registerJmxBean(oname, checkAdapter);
    }

    private void unregisterAdapter(CheckAdapter checkAdapter) throws JMException {
        unregisterJmxBean(new ObjectName(namingStrategy.buildJMXName(checkAdapter)));
    }

    private void registerJmxBean(ObjectName oname, Object adapter) throws JMException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.registerMBean(adapter, oname);
            LOG.debug("Register JMX bean: " + oname);
        } catch (InstanceAlreadyExistsException e) {
            String message = "JMX bean with this name already exists. "
                    + "This may happen by an configuration error or when an context configuration will be instantiated more times! ";
            LOG.warn(message + oname);
            LOG.debug("", e);
        }
    }

    private void unregisterJmxBean(ObjectName oname) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.unregisterMBean(oname);
            LOG.info("Unregister JMX bean: " + oname);
        } catch (JMException e) {
            LOG.warn("Error during unregistering MBean {}: {}", oname, e.getMessage());
        }
    }

}
