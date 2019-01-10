package com.ecg.replyts.core.api.configadmin;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author mhuttar
 * <p>
 * Refers to a particular {@link PluginConfiguration} in the set of filters a tenant configured.
 *
 * <p>
 * The *tenant* identifies a {@link PluginConfiguration} by referring to the {@link ConfigurationLabel}. However, it can update
 * the {@link PluginConfiguration} (e.g. change the json part of that config), while keeping the {@link ConfigurationLabel} stable
 * over time. That is, the tenant has a mutable perspective on the {@link PluginConfiguration}s.
 *
 * <p>
 * However, internally in Comaas, we want to be able to reason about a particular filter configuration, with an id that changes if the
 * configuration changes. So if a {@link PluginConfiguration} is updated, it will be a filter with a new id.
 *
 * <p>
 * TODO: implement the new UUID: See {@link PluginConfiguration#getUuid()}
 * <p>
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