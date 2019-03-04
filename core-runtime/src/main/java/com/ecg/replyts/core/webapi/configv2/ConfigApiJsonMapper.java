package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ecg.unicom.comaas.configv2.model.FilterConfig;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

public class ConfigApiJsonMapper {

    static class ToJson {
        // Note: we adopted a spec-based model, so we can extend/rewrite the mapper to use OpenAPI generated FilterConfig DAO objects,
        // instead of ObjectNodes

        /**
         * configurations as a json object
         */
        public static ObjectNode pluginConfigurationList(List<PluginConfiguration> configList) {
            ArrayNode arrayNode = JsonObjects.newJsonArray();

            configList.stream().forEach(pluginConf -> {
                ObjectNode config = pluginConfig(pluginConf);
                arrayNode.add(config);
            });

            return JsonObjects.builder().attr("configs", arrayNode).build();
        }

        /**
         * For backwards-compatibility reasons, we didn't add the UUID to the API json model
         */
        public static ObjectNode pluginConfig(PluginConfiguration pluginConfiguration) {
            return JsonObjects.builder()
                    .attr("pluginFactory", pluginConfiguration.getId().getPluginFactory())
                    .attr("instanceId", pluginConfiguration.getId().getInstanceId())
                    .attr("priority", pluginConfiguration.getPriority())
                    .attr("state", pluginConfiguration.getState().name())
                    .attr("version", pluginConfiguration.getVersion())
                    .attr("configuration", pluginConfiguration.getConfiguration())
                    .build();
        }

        /**
         * For backwards-compatibility reasons, we didn't add the UUID to the API json model
         */
        public static FilterConfig filterConfig(PluginConfiguration pc) {
            return new
                    FilterConfig()
                    .instanceId(pc.getId().getInstanceId())
                    .pluginFactory(pc.getId().getPluginFactory())
                    .state(state(pc.getState()))
                    .version(pc.getVersion())
                    .priority(pc.getPriority())
                    ._configuration(pc.getConfiguration());
        }

        public static FilterConfig.StateEnum state(PluginState state){
            return FilterConfig.StateEnum.fromValue(state.toString());
        }

    }

    static class ToModel {
        private static final ObjectMapper om = new ObjectMapper();

        public static List<PluginConfiguration> pluginConfigurationList(JsonNode body) throws Exception {
            Objects.requireNonNull(body, "Need a non-null body");
            switch (body.getNodeType()) {
                case ARRAY: // do not remove this case: it's the protocol used by the tenants (different from output format!)
                    return pluginConfigurationListFromArray(body);
                case OBJECT:
                    return pluginConfigurationListFromConfigObject(body);
                default:
                    throw new RuntimeException("Input body is of type " + body.getNodeType() + "; expected array");
            }
        }

        public static PluginConfiguration pluginConfig(FilterConfig fc, UUID filterId) {
            return PluginConfiguration.create(
                    filterId,
                    new ConfigurationId(
                            fc.getPluginFactory(), fc.getInstanceId()
                    ),
                    fc.getPriority(),
                    state(fc.getState()),
                    fc.getVersion(),
                    om.valueToTree( fc.getConfiguration())
            );
        }

        public static PluginState state(FilterConfig.StateEnum state){
            return PluginState.valueOf(state.toString());
        }

        /**
         * convenience case to allow parsing the output-format of the Model-to-JSON mapper, which is exposed to tenants.
         */
        public static List<PluginConfiguration> pluginConfigurationListFromConfigObject(JsonNode body) throws Exception {
            JsonNode config = Objects.requireNonNull(body.get("configs"), "No field '.configs'");
            return pluginConfigurationListFromArray(config);
        }

        public static List<PluginConfiguration> pluginConfigurationListFromArray(JsonNode body) throws Exception {
            List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

            for (JsonNode node : body) {
                assertArrayElement(node, "pluginFactory", TextNode.class);
                assertArrayElement(node, "instanceId", TextNode.class);

                String pluginFactory = node.get("pluginFactory").textValue();
                String instanceId = node.get("instanceId").textValue();
                JsonNode configuration = node.get("configuration");

                PluginConfiguration config = pluginConfig(pluginFactory, instanceId, configuration, configuration);
                pluginConfigurations.add(config);
            }
            return pluginConfigurations;
        }

        protected static PluginConfiguration pluginConfig(String pluginFactory, String instanceId, JsonNode body, JsonNode configuration) throws IllegalArgumentException, ClassNotFoundException {
            if (configuration == null) {
                throw new IllegalArgumentException("payload needs a configuration node where the filter configuration is in");
            }

            Long priority = Long.valueOf(getContent(body, "priority", "0"));
            PluginState state = PluginState.valueOf(getContent(body, "state", PluginState.ENABLED.name()));

            return PluginConfiguration.create(UUID.randomUUID(), new ConfigurationId(pluginFactory, instanceId), priority, state, 1L, configuration);
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
