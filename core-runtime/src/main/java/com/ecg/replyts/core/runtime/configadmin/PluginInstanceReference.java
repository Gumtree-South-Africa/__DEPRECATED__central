package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;

import java.util.Comparator;

/**
 * Refers to a plugin instance and adds some extra information to the plugin that is necessary for the
 * {@link ConfigurationAdmin} but not necessary for the plugin instance itself. (e.g. the priority and state of a
 * plugin).
 *
 * @param <T>
 * @author mhuttar
 */
public class PluginInstanceReference<T> {
    /**
     * Comparator to order plugins based on their priority. Higher priority values will be at the start of the list. If priorities are pairsAreEqual,
     * plugins are sorted by class and instance name.
     */
    public static final Comparator<PluginInstanceReference<?>> PLUGIN_REF_ORDERING_COMPARATOR = new Comparator<PluginInstanceReference<?>>() {
        public int compare(PluginInstanceReference<?> o1,
                           PluginInstanceReference<?> o2) {
            int priority = (int) Math.signum(o2.config.getPriority()
                    - o1.config.getPriority());

            if (priority != 0) {
                return priority;
            }

            ConfigurationId firstId = o1.config.getId();
            ConfigurationId secondId = o2.config.getId();

            if (!firstId.getPluginFactory().equals(secondId.getPluginFactory())) {
                return firstId.getPluginFactory().getName().compareTo(secondId.getPluginFactory().getName());
            }
            return firstId.getInstanceId().compareTo(secondId.getInstanceId());

        }
    };

    private final PluginConfiguration config;
    private final T createdService;

    public PluginInstanceReference(PluginConfiguration config, T createdService) {
        this.config = config;
        this.createdService = createdService;
    }

    public T getCreatedService() {
        return createdService;
    }

    public PluginState getState() {
        return config.getState();
    }

    public PluginConfiguration getConfiguration() {
        return config;
    }

    public String toString() {
        return config.getId().toString();
    }
}