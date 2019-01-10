package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;

public class PluginConfiguration {
    private final ConfigurationLabel label;
    private final long version;
    private final long priority;
    private final PluginState state;
    private final JsonNode configuration;

    public PluginConfiguration(ConfigurationLabel label, long priority, PluginState state, long version, JsonNode configuration) {
        this.label = label;
        this.priority = priority;
        this.version = version;
        this.state = state;
        this.configuration = configuration;
    }

    public ConfigurationLabel getLabel() {
        return label;
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
