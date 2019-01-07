package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationLabel;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ConfigApiJsonMapper {

    static class Model {

        /**
         * configurations as a json object
         */
        public static ObjectNode toJsonPluginConfigurationList(List<PluginConfiguration> configList) {
            ArrayNode arrayNode = JsonObjects.newJsonArray();
            for (PluginConfiguration pluginConfiguration : configList) {
                ObjectNode config = toJsonPluginConfiguration(pluginConfiguration);
                arrayNode.add(config);
            }
            return JsonObjects.builder().attr("configs", arrayNode).build();
        }
        public static ObjectNode toJsonPluginConfiguration(PluginConfiguration pluginConfiguration){
            return JsonObjects.builder()
                    .attr("pluginFactory", pluginConfiguration.getLabel().getPluginFactory())
                    .attr("instanceId", pluginConfiguration.getLabel().getInstanceId())
                    .attr("priority", pluginConfiguration.getPriority())
                    .attr("state", pluginConfiguration.getState().name())
                    .attr("version", pluginConfiguration.getVersion())
                    .attr("configuration", pluginConfiguration.getConfiguration())
                    .build();
        }

    }

    static class Json {

        public static List<PluginConfiguration> toPluginConfigurationList(ArrayNode body) throws Exception {
            List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

            for (JsonNode node : body) {
                assertArrayElement(node, "pluginFactory", TextNode.class);
                assertArrayElement(node, "instanceId", TextNode.class);

                String pluginFactory = node.get("pluginFactory").textValue();
                String instanceId = node.get("instanceId").textValue();
                JsonNode configuration = node.get("configuration");

                PluginConfiguration config = toPluginConfiguration(pluginFactory, instanceId, configuration, configuration);
                pluginConfigurations.add(config);
            }
            return pluginConfigurations;
        }

        protected static PluginConfiguration toPluginConfiguration(String pluginFactory, String instanceId, JsonNode body, JsonNode configuration) throws IllegalArgumentException, ClassNotFoundException {
            if (configuration == null) {
                throw new IllegalArgumentException("payload needs a configuration node where the filter configuration is in");
            }

            Long priority = Long.valueOf(getContent(body, "priority", "0"));
            PluginState state = PluginState.valueOf(getContent(body, "state", PluginState.ENABLED.name()));

            return PluginConfiguration.createNewPluginConfiguration(new ConfigurationLabel(pluginFactory, instanceId), priority, state, 1L, configuration);
        }

        private static String getContent(JsonNode body, String fieldname, String alternative) {
            JsonNode fn = body.get(fieldname);
            return fn == null ? alternative : fn.asText();
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
    }
}
