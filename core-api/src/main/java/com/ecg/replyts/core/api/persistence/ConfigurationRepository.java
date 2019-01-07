package com.ecg.replyts.core.api.persistence;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
     * creates/updates a configuration. Whether to create or to update a configuration depends on the Configuration's
     * {@link ConfigurationId}
     */
    void persistConfiguration(PluginConfiguration configuration, String remoteAddress);

    /**
     * deletes the configuration identified by this {@link ConfigurationId}.
     */
    void deleteConfiguration(String pluginFactory, String instanceId, String remoteAddress);

    void replaceConfigurations(List<PluginConfiguration> pluginConfigurations, String remoteAddress);

}
