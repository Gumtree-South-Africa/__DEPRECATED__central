package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uniquely identifies a plugin instance from a particular service factory.
 *
 * @author mhuttar
 */
public class ConfigurationId {
    private final String pluginFactory;
    private final String instanceId;

    public ConfigurationId(@Nonnull Class<? extends BasePluginFactory<?>> pluginFactory, @Nonnull String instanceId) {
        this(pluginFactory.getName(), instanceId);
    }

    public ConfigurationId(@Nonnull String pluginFactory, @Nonnull String instanceId) {
        this.pluginFactory = checkNotNull(pluginFactory, "pluginFactory");
        this.instanceId = checkNotNull(instanceId, "instanceId");
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPluginFactory() {
        return pluginFactory;
    }

    @SuppressWarnings("unchecked") // the cast is supposed to be safe (statically) due to the constructor signature
    public Class<? extends BasePluginFactory<?>> findFactoryClass() {
        try {
            return (Class<? extends BasePluginFactory<?>>) Class.forName(pluginFactory);
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationId that = (ConfigurationId) o;
        return Objects.equal(pluginFactory, that.pluginFactory) &&
                Objects.equal(instanceId, that.instanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pluginFactory, instanceId);
    }

    @Override
    public String toString() {
        return String.format("ConfigurationId(%s:%s)", pluginFactory, instanceId);
    }

}