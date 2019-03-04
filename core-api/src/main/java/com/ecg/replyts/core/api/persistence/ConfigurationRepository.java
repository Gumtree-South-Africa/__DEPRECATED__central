package com.ecg.replyts.core.api.persistence;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;

import java.util.List;

/**
 * Persists all sorts of dynamically updateable configurations. This repository is constantly polled by the
 * Configuration Admin to see if new filter configurations are available.
 */
public interface ConfigurationRepository {
    /**
     * @return a list of all configurations for all services known/available. the order of this list is not specified.
     */
    List<PluginConfiguration> getConfigurations();

    /**
     * creates/updates a single configuration, adding it to the list of existing configuration.
     * If the {@link ConfigurationId} already exists, it is replaced.
     */
    void upsertConfiguration(PluginConfiguration configuration, String remoteAddress);

    /**
     * deletes the configuration identified by this {@link ConfigurationId}.
     */
    void deleteConfiguration(String pluginFactory, String instanceId, String remoteAddress);

    /**
     * make all existing configurations obsolete, replacing them with the given ones.
     */
    void replaceConfigurations(List<PluginConfiguration> pluginConfigurations, String remoteAddress);

}
