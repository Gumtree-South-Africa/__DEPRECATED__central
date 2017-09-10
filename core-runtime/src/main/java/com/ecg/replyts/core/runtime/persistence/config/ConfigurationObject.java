package com.ecg.replyts.core.runtime.persistence.config;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;

class ConfigurationObject {
    private final long timestamp;

    private final PluginConfiguration pluginConfiguration;

    ConfigurationObject(long timestamp, PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }
}
