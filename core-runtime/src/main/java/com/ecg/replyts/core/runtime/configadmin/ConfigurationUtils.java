package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ConfigurationUtils {

    public static PluginConfiguration extract(String pluginFactory, String instanceId, JsonNode body, JsonNode configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("payload needs a configuration node where the filter configuration is in");
        }

        long priority = Long.parseLong(getContent(body, "priority", "0"));
        PluginState state = PluginState.valueOf(getContent(body, "state", PluginState.ENABLED.name()));

        ConfigurationId configId = new ConfigurationId(pluginFactory, instanceId);
        return new PluginConfiguration(configId, priority, state, 1L, configuration);
    }

    public static List<PluginConfiguration> verify(ConfigurationValidator validator, ArrayNode body) {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (JsonNode node : body) {
            assertArrayElement(node, "pluginFactory", TextNode.class);
            assertArrayElement(node, "instanceId", TextNode.class);

            String pluginFactory = node.get("pluginFactory").textValue();
            String instanceId = node.get("instanceId").textValue();
            JsonNode configuration = node.get("configuration");

            PluginConfiguration config = ConfigurationUtils.extract(
                    pluginFactory, instanceId, configuration, configuration);

            if (!validator.validateConfiguration(config)) {
                throw new IllegalArgumentException(format("PluginFactory %s not found", pluginFactory));
            }
            pluginConfigurations.add(config);
        }
        return pluginConfigurations;
    }

    private static void assertArrayElement(JsonNode element, String fieldName, Class<? extends JsonNode> clazz) {
        if (!(element instanceof ObjectNode)) {
            throw new IllegalArgumentException("Array element is not an Object");
        }

        JsonNode field = element.get(fieldName);

        if (!field.getClass().equals(clazz)) {
            throw new IllegalArgumentException(format("Array element's contents does not contain %s as a %s class", fieldName, clazz));
        }

        if (field instanceof TextNode && !StringUtils.hasText(field.textValue())) {
            throw new IllegalArgumentException(format("Array element's contents contains field %s but it contains an empty value", fieldName));
        }
    }

    private static String getContent(JsonNode body, String fieldname, String alternative) {
        JsonNode fn = body.get(fieldname);
        return fn == null ? alternative : fn.asText();
    }
}
