package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationLabel;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ConfigApiJsonMapperTest {

    @Test
    public void properlyMapsModelToJson() {
        List<PluginConfiguration> configList = Collections.singletonList(
                PluginConfiguration.createNewPluginConfiguration(
                        new ConfigurationLabel("filterFactory", "fooinstance"),
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

    public class TestRepository implements ConfigurationRepository {
        @Override
        public List<PluginConfiguration> getConfigurations() {
            return null;
        }

        @Override
        public void persistConfiguration(PluginConfiguration configuration, String remoteAddr) {
            throw new NotImplementedException("No");
        }

        @Override
        public void deleteConfiguration(String pluginFactory, String instanceId, String remoteAddress) {
            throw new NotImplementedException("No");
        }

        @Override
        public void replaceConfigurations(List<PluginConfiguration> pluginConfigurations, String remoteAddr) {
            throw new NotImplementedException("No");
        }
    }
}