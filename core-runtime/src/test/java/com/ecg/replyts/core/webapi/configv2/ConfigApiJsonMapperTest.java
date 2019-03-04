package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ecg.unicom.comaas.configv2.model.FilterConfig;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static ecg.unicom.comaas.configv2.model.FilterConfig.StateEnum.ENABLED;
import static org.junit.Assert.assertEquals;

public class ConfigApiJsonMapperTest {

    @Test
    public void toJsonPluginConfig() {
        List<PluginConfiguration> configList = Collections.singletonList(
                PluginConfiguration.createWithRandomUuid(
                        new ConfigurationId("filterFactory", "fooinstance"),
                        222L, PluginState.ENABLED, 12L, JsonObjects.parse("{foo: 322}")
                )
        );

        // execute SUT
        ObjectNode listConfigurations = ConfigApiJsonMapper.ToJson.pluginConfigurationList(configList);

        ArrayNode configs = (ArrayNode) listConfigurations.get("configs");
        ObjectNode firstConfig = (ObjectNode) configs.get(0);

        assertEquals(1, configs.size());
        assertEquals("ENABLED", firstConfig.get("state").textValue());
        assertEquals(12L, firstConfig.get("version").longValue());
        assertEquals(222L, firstConfig.get("priority").longValue());
        assertEquals("filterFactory", firstConfig.get("pluginFactory").textValue());
        assertEquals("fooinstance", firstConfig.get("instanceId").textValue());
        assertEquals(322, firstConfig.get("configuration").get("foo").intValue());
    }

    @Test
    public void toJsonFilterConfig() {
        UUID pluginUUID = UUID.randomUUID();

        final JsonNode filterConfig = new ObjectMapper().createObjectNode().put("k", "v");
        PluginConfiguration input = PluginConfiguration.create(
                pluginUUID,
                new ConfigurationId(
                        "my.plugin.factory", "myInstanceId"
                ),
                3,
                PluginState.ENABLED,
                2,
                filterConfig
        );

        // execute SUT
        FilterConfig actual = ConfigApiJsonMapper.ToJson.filterConfig(input);

        // map in other direction, as the toModel mapper enforces correct state.
        // This would fail if JSON model is not fully populated.
        // Not comparing input/output, because PluginConfiguration#equals not implemented
        PluginConfiguration output = ConfigApiJsonMapper.ToModel.pluginConfig(actual, pluginUUID);

        // validate SUT response
        assertEquals(
                new FilterConfig()
                        .instanceId(input.getId().getInstanceId())
                        .pluginFactory(input.getId().getPluginFactory())
                        .priority(input.getPriority())
                        .state(ENABLED)
                        .version(input.getVersion())
                        ._configuration(filterConfig),
                actual
        );

    }

}