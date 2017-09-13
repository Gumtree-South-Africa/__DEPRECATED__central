package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages Configurations/Plugins for a specific type. Will keep an ordered list of all plugins running and is able to
 * add, update or remove any on them when configuration changes.
 * <p/>
 * Each Configuration Admin is only responsible for the list of {@link BasePluginFactory} instances, it receives. It
 * will reject all configurations for service factories, it does not know of.
 *
 * @param <T>
 * @author mhuttar
 */
public class ConfigurationAdmin<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationAdmin.class);

    @Autowired
    private ClusterRefreshPublisher clusterRefreshPublisher;

    private Map<ConfigurationId, PluginInstanceReference<T>> knownServices = new HashMap<>();

    private AtomicReference<List<PluginInstanceReference<T>>> runningServices = new AtomicReference() {{
        set(new ArrayList<>());
    }};

    private List<BasePluginFactory<T>> factories;

    private String adminName;

    public ConfigurationAdmin(List<BasePluginFactory<T>> factories, String adminName) {
        this.factories = factories;
        this.adminName = adminName;

        LOG.info("Starting Configuration Admin '{}' with known Factories {}", adminName, factories);
    }

    String getAdminName() {
        return adminName;
    }

    /**
     * registers a new configuration and launches a plugin instance for it. The service will be immediately started and
     * in action on this machine after the method was called. . Throws an Illegal State Exception if it does not have a
     * reference to the factory required by the service.
     *
     * @param configuration configuration to launch/update service.
     */
    void putConfiguration(PluginConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration required");
        }
        ConfigurationId id = configuration.getId();
        T createdService = createPluginInstance(configuration);

        PluginInstanceReference<T> sr = new PluginInstanceReference<>(configuration, createdService);
        synchronized (this) {
            knownServices.put(id, sr);
            LOG.info("Adding/Updating Configuration {}", id);
            updateConfiguration();
        }
    }

    /**
     * queries this config admin, if it knows a {@link BasePluginFactory} that is referred by the configuration's plugin
     * factory.
     *
     * @return <code>true</code> if this instance has a reference to the plugin factory this configuration is for.
     */
    boolean handlesConfiguration(ConfigurationId configId) {
        for (BasePluginFactory<?> s : factories) {
            if (s.getClass().equals(configId.findFactoryClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if this configuration admin has a specific configuration running.
     *
     * @param config configuration id to check for
     * @return <code>true</code> if this configuration exists and is alive.
     */
    public boolean isRunning(ConfigurationId config) {
        synchronized (this) {
            return knownServices.containsKey(config);
        }
    }

    /**
     * Creates a plugin instance from the given configuration but does not register it. Can be used externally to
     * validate a new plugin configuration.
     */
    @SuppressWarnings("unchecked")
    T createPluginInstance(PluginConfiguration configuration) {
        ConfigurationId id = configuration.getId();
        for (BasePluginFactory<?> s : factories) {
            if (s.getClass().equals(id.findFactoryClass())) {
                return (T) s.createPlugin(id.getInstanceId(), configuration.getConfiguration());
            }
        }
        throw new IllegalStateException(String.format("ServiceFactory %s not found. Cannot create a new service from it", id.getPluginFactory()));
    }

    /**
     * removes a service from the list of services and disposes it.
     *
     * @param configurationId service to be removed.
     */
    public void deleteConfiguration(ConfigurationId configurationId) {
        synchronized (this) {
            if (knownServices.containsKey(configurationId)) {
                knownServices.remove(configurationId);
                LOG.info(": {}", configurationId);
                updateConfiguration();
            }
        }
        clusterRefreshPublisher.publish();
    }

    // SYNCHRONIZED!
    private void updateConfiguration() {
        List<PluginInstanceReference<T>> newServiceList = new ArrayList<>(knownServices.values());
        newServiceList.sort(PluginInstanceReference.PLUGIN_REF_ORDERING_COMPARATOR);
        runningServices.set(Collections.unmodifiableList(newServiceList));
    }

    /**
     * @return priority ordered list of all services this {@link ConfigurationAdmin} knows of.
     */
    public List<PluginInstanceReference<T>> getRunningServices() {
        return runningServices.get();
    }
}