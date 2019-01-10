package com.ecg.replyts.core.api.configadmin;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uniquely identifies a plugin instance from a particular service factory.
 *
 * @author mhuttar
 */
public class ConfigurationLabel {
    private final String pluginFactory;
    private final String instanceId;

    public ConfigurationLabel(@Nonnull String pluginFactory, @Nonnull String instanceId) {
        this.pluginFactory = checkNotNull(pluginFactory, "pluginFactory");
        this.instanceId = checkNotNull(instanceId, "instanceId");
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPluginFactory() {
        return pluginFactory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationLabel that = (ConfigurationLabel) o;
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