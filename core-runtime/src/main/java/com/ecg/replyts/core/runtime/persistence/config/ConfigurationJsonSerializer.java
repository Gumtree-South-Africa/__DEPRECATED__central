package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.convert.ConversionException;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class ConfigurationJsonSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationJsonSerializer.class);

    /**
     * Converts {@link Configurations} to a JSON String.
     */
    public String fromDomain(Configurations configurations) {
        ArrayNode configurationObjects = JsonObjects.newJsonArray();

        for (ConfigurationObject configurationObject : configurations.getConfigurationObjects()) {
            PluginConfiguration pc = configurationObject.getPluginConfiguration();
            ObjectNode serializedConfig = JsonObjects.builder()
                    .attr("config", pc.getConfiguration())
                    .attr("pluginFactory", pc.getId().getPluginFactory().getName())
                    .attr("instanceId", pc.getId().getInstanceId())
                    .attr("priority", pc.getPriority())
                    .attr("version", configurationObject.getTimestamp())
                    .attr("state", pc.getState().name())
                    .attr("timestamp", configurationObject.getTimestamp()).build();
            configurationObjects.add(serializedConfig);
        }

        return configurationObjects.toString();
    }

    /**
     * Converts a JSON String to a {@link Configurations}.
     */
    public Configurations toDomain(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            LOG.warn("Found empty configurations: no filters will be configured");
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

    private ConfigurationObject loadPluginConfiguration(JsonNode configNode) {
        ConfigurationId configId = extractConfigurationId(configNode);
        String priority = configNode.get("priority").toString();
        String version = configNode.get("version").toString();
        String state = configNode.get("state").textValue();
        long timestamp = configNode.get("timestamp") == null ? 0 : configNode.get("timestamp").asLong(0l);
        JsonNode configuration = configNode.get("config");
        long pluginPriority = Long.parseLong(priority);
        PluginState pluginState = PluginState.valueOf(state);
        long configurationVersion = Long.parseLong(version);
        PluginConfiguration pluginConfiguration =
                new PluginConfiguration(configId, pluginPriority, pluginState, configurationVersion, configuration);

        return new ConfigurationObject(timestamp, pluginConfiguration);
    }

    private ConfigurationId extractConfigurationId(JsonNode node) {
        String instanceId = node.get("instanceId").asText();
        String pluginFactoryType = node.get("pluginFactory").asText()
                .replace("com.ecg.de.ebayk.messagecenter.filters", "com.ecg.messagecenter.filters");
        try {
            Class<? extends BasePluginFactory<?>> pluginFactory = (Class<? extends BasePluginFactory<?>>) Class.forName(pluginFactoryType);
            return new ConfigurationId(pluginFactory, instanceId);
        } catch (ClassNotFoundException e) {
            throw new ConversionException("can not address Plugin factory of type: " + pluginFactoryType + " and instanceId: " + instanceId, e);
        }
    }

}
