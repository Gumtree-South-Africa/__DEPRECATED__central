package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;

/**
 * Uniquely identifies a plugin instance from a particular service factory.
 *
 * @author mhuttar
 */
public class ConfigurationId {
    private final Class<? extends BasePluginFactory<?>> pluginFactory;
    private final String instanceId;

    public ConfigurationId(Class<? extends BasePluginFactory<?>> pluginFactory, String instanceId) {
        if (pluginFactory == null || instanceId == null) {
            throw new IllegalArgumentException();
        }
        this.pluginFactory = pluginFactory;
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Class<? extends BasePluginFactory<?>> getPluginFactory() {
        return pluginFactory;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + instanceId.hashCode();
        result = prime * result + pluginFactory.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigurationId other = (ConfigurationId) obj;

        return pluginFactory.equals(other.pluginFactory) && instanceId.equals(other.instanceId);
    }

    @Override
    public String toString() {
        return String.format("ConfigurationId(%s:%s)", pluginFactory, instanceId);
    }

}