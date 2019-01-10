package com.ecg.replyts.core.runtime.persistence.config;

import com.ecg.replyts.core.api.configadmin.ConfigurationLabel;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import java.util.List;

public abstract class ConfigurationJsonSerializer {
    public static String fromDomain(Configurations configurations) {
        ArrayNode configurationObjects = JsonObjects.newJsonArray();

        for (ConfigurationObject configurationObject : configurations.getConfigurationObjects()) {
            PluginConfiguration pluginConfiguration = configurationObject.getPluginConfiguration();
            ObjectNode serializedConfig = JsonObjects.builder()
                    .attr("config", pluginConfiguration.getConfiguration())
                    .attr("pluginFactory", pluginConfiguration.getLabel().getPluginFactory())
                    .attr("instanceId", pluginConfiguration.getLabel().getInstanceId())
                    .attr("priority", pluginConfiguration.getPriority())
                    .attr("version", configurationObject.getTimestamp())
                    .attr("state", pluginConfiguration.getState().name())
                    .attr("timestamp", configurationObject.getTimestamp())
                    .build();
            configurationObjects.add(serializedConfig);
        }

        return configurationObjects.toString();
    }

    public static Configurations toDomain(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return Configurations.EMPTY_CONFIG_SET;
        } else {
            ArrayNode configurationArray = (ArrayNode) JsonObjects.parse(jsonString);
            List<ConfigurationObject> configs = Lists.newArrayList();
            for (JsonNode configNode : configurationArray) {
                configs.add(loadPluginConfiguration(configNode));
            }
            return new Configurations(configs);
        }
    }

    private static ConfigurationObject loadPluginConfiguration(JsonNode configNode) {
        ConfigurationLabel configId = extractConfigurationId(configNode);
        String priority = configNode.get("priority").toString();
        String version = configNode.get("version").toString();
        String state = configNode.get("state").textValue();
        long timestamp = configNode.get("timestamp") == null ? 0 : configNode.get("timestamp").asLong(0L);
        JsonNode configuration = configNode.get("config");
        long pluginPriority = Long.parseLong(priority);
        PluginState pluginState = PluginState.valueOf(state);
        long configurationVersion = Long.parseLong(version);
        PluginConfiguration pluginConfiguration = new PluginConfiguration(configId, pluginPriority, pluginState, configurationVersion, configuration);

        return new ConfigurationObject(timestamp, pluginConfiguration);
    }

    private static ConfigurationLabel extractConfigurationId(JsonNode node) {
        String instanceId = node.get("instanceId").asText();
        String pluginFactoryType = node.get("pluginFactory").asText()
                .replace("com.ecg.de.ebayk.messagecenter.filters", "com.ecg.messagecenter.filters");
        return new ConfigurationLabel(pluginFactoryType, instanceId);
    }
}
