package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.UUID;

public class PluginConfiguration {
    private final UUID uuid;
    private final ConfigurationLabel label;
    private final long version;
    private final long priority;
    private final PluginState state;
    private final JsonNode configuration;

    public PluginConfiguration(UUID uuid, ConfigurationLabel label, long priority, PluginState state, long version, JsonNode configuration) {
        this.uuid = Objects.requireNonNull(uuid);
        this.label = label;
        this.priority = priority;
        this.version = version;
        this.state = state;
        this.configuration = configuration;
    }

    /**
     * Create new Plugin Configuration with unique ID
     */
    public static PluginConfiguration createNewPluginConfiguration(ConfigurationLabel label, long priority, PluginState state, long version, JsonNode configuration) {
        return new PluginConfiguration(UUID.randomUUID(), label, priority, state, version, configuration);
    }

    public UUID getUuid() {
        return uuid;
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
