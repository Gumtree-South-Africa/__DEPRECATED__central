package com.ecg.replyts.client.configclient;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Configuration for a plugin or a filter. In this documentation, the term "plugin" is an abstraction for filters
 * and resultinspectors (or any other types of plugins, that may be available in the future)
 */
public class Configuration {

    private final long priority;
    private final PluginState state;
    private final JsonNode configuration;
    private final ConfigurationId configurationId;

    /**
     * creates a new configuration
     *
     * @param configurationId identifies the plugin this configuration applies for. Consists of a plugin factory and an
     *                        instance name (plugin factories can have many instances). Every ConfigurationId object is
     *                        a unique identifier for a plugin.
     * @param state           describes the state of the plugin
     * @param priority        all plugins are ordered by their priority. Normally priorities should not really matter, use
     *                        with care therefore.
     * @param configuration   actual configuration (in json) the plugin factory will receive to create a newly configured plugin instance.
     */
    public Configuration(ConfigurationId configurationId, PluginState state, long priority, JsonNode configuration) {
        this.configuration = configuration;
        this.state = state;
        this.priority = priority;
        this.configurationId = configurationId;
    }

    public long getPriority() {
        return priority;
    }

    public PluginState getState() {
        return state;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public ConfigurationId getConfigurationId() {
        return configurationId;
    }

    /**
     * Unique identifier for a plugin
     */
    public static class ConfigurationId {
        private final String pluginFactory;
        private final String instanceId;

        public String getPluginFactory() {
            return pluginFactory;
        }

        public String getInstanceId() {
            return instanceId;
        }

        /**
         * creates a new configuration id
         *
         * @param pluginFactory fully qualified class name of the plugin factory. The plugin factory must be in ReplyTS
         *                      classpath
         * @param instanceId    name of the plugin instance. The (pluginFactory, instanceId) tuple is guaranteed to uniquely
         *                      identify a plugin instnace. (there can not be two plugin instances with the same name for one
         *                      plugin factory)
         */
        public ConfigurationId(String pluginFactory, String instanceId) {
            this.instanceId = instanceId;
            this.pluginFactory = pluginFactory;
        }
    }


}