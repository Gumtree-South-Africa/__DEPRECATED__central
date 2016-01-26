package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;

public class PluginConfiguration {

    private final ConfigurationId id;
    private long version;
    private final long priority;
    private final PluginState state;
    private final JsonNode configuration;


    public PluginConfiguration(ConfigurationId id, long priority, PluginState state, long version, JsonNode configuration) {
        this.id = id;
        this.priority = priority;
        this.version = version;
        this.state = state;
        this.configuration = configuration;
    }

    public ConfigurationId getId() {
        return id;
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

    public long getVersion() {
        return version;
    }

}
