package com.ecg.replyts.core.api.persistence;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationRepositoryTest {
    @Spy
    private TestRepository repository;

    @Before
    public void setup() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration(
                new ConfigurationId(FilterFactory.class, "fooinstance"),
                222L, PluginState.ENABLED, 12L, JsonObjects.parse("{foo: 322}")
        );
        List<PluginConfiguration> configList = Collections.singletonList(pluginConfiguration);
        when(repository.getConfigurations()).thenReturn(configList);
    }

    @Test
    public void listsExistingConfigurations() {
        ObjectNode listConfigurations = repository.getConfigurationsAsJson();

        ArrayNode configs = (ArrayNode) listConfigurations.get("configs");
        ObjectNode firstConfig = (ObjectNode) configs.get(0);

        assertEquals(1, configs.size());
        assertEquals("ENABLED", firstConfig.get("state").textValue());
        assertEquals(12L, firstConfig.get("version").longValue());
        assertEquals(222L, firstConfig.get("priority").longValue());
        assertEquals(FilterFactory.class.getName(), firstConfig.get("pluginFactory").textValue());
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