package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConfigApiJsonMapperTest {

    @Test
    public void properlyMapsModelToJson() {
        List<PluginConfiguration> configList = Collections.singletonList(
                PluginConfiguration.createGeneratingUuid(
                        new ConfigurationId("filterFactory", "fooinstance"),
                        222L, PluginState.ENABLED, 12L, JsonObjects.parse("{foo: 322}")
                )
        );

        // execute SUT
        ObjectNode listConfigurations = ConfigApiJsonMapper.Model.toJsonPluginConfigurationList(configList);

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

}