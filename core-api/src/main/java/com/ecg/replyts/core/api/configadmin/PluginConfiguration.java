package com.ecg.replyts.core.api.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;
import java.util.UUID;

public class PluginConfiguration {
    private final UUID uuid;
    private final ConfigurationId id;
    private final long version;
    private final long priority;
    private final PluginState state;
    private final JsonNode configuration;

    private PluginConfiguration(UUID uuid, ConfigurationId id, long priority, PluginState state, long version, JsonNode configuration) {
        this.uuid = Objects.requireNonNull(uuid);
        this.id = id;
        this.priority = priority;
        this.version = version;
        this.state = state;
        this.configuration = configuration;
    }

    @Deprecated
    public static PluginConfiguration createGeneratingUuid(ConfigurationId id, long priority, PluginState state, long version, JsonNode configuration) {
        return new PluginConfiguration(UUID.randomUUID(), id, priority, state, version, configuration);
    }


    public static PluginConfiguration create(UUID uuid, ConfigurationId id, long priority, PluginState state, long version, JsonNode configuration) {
        return new PluginConfiguration(uuid, id, priority, state, version, configuration);
    }

    public UUID getUuid() {
        return uuid;
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
