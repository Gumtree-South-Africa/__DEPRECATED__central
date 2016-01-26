package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigCrudControllerTest {


    private ConfigCrudController configCrudController;

    @Mock
    private ConfigurationRepository repository;

    @Mock
    private ConfigurationUpdateNotifier updateNotifier;

    @Mock
    private PluginConfiguration pc;

    private List<PluginConfiguration> configList = new ArrayList<PluginConfiguration>();

    private JsonNode validPutData;

    @Before
    public void setup() throws Exception {
        configCrudController = new ConfigCrudController(repository, updateNotifier);
        when(repository.getConfigurations()).thenReturn(configList);
        configList.add(pc);
        when(pc.getId()).thenReturn(new ConfigurationId(FilterFactory.class, "fooinstance"));
        when(pc.getVersion()).thenReturn(12l);
        when(pc.getPriority()).thenReturn(222l);
        when(pc.getState()).thenReturn(PluginState.ENABLED);
        when(pc.getConfiguration()).thenReturn(JsonObjects.parse("{foo: 322}"));

        when(updateNotifier.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenReturn(true);

        validPutData = JsonObjects.builder().attr("configuration", JsonObjects.newJsonObject()).build();
    }

    @Test
    public void listsExistingConfigurations() {
        ObjectNode listConfigurations = configCrudController.listConfigurations();

        ArrayNode configs = (ArrayNode) listConfigurations.get("configs");
        ObjectNode firstConfig = (ObjectNode) configs.get(0);

        assertEquals(1, configs.size());
        assertEquals("ENABLED", firstConfig.get("state").textValue());
        assertEquals(12l, firstConfig.get("version").longValue());
        assertEquals(222l, firstConfig.get("priority").longValue());
        assertEquals(FilterFactory.class.getName(), firstConfig.get("pluginFactory").textValue());
        assertEquals("fooinstance", firstConfig.get("instanceId").textValue());
        assertEquals(322, firstConfig.get("configuration").get("foo").intValue());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForEmptyInstanceId() throws Exception {
        configCrudController.addConfiguration(FilterFactory.class.getName(), null, JsonObjects.newJsonObject());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForNonexistantPluginFactory() throws Exception {
        configCrudController.addConfiguration("com.goo.bar", "foo", JsonObjects.newJsonObject());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForUnknownPluginFactory() throws Exception {
        when(updateNotifier.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenReturn(false);
        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidConfig() throws Exception {
        when(updateNotifier.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenThrow(new IllegalStateException());
        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
    }

    @Test
    public void persistsValidConfiguration() throws Exception {
        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
        verify(repository).persistConfiguration(Mockito.any(PluginConfiguration.class));
    }

    @Test
    public void deltesContent() throws Exception {
        configCrudController.deleteConfiguration(FilterFactory.class.getName(), "iid");
        verify(repository).deleteConfiguration(new ConfigurationId(FilterFactory.class, "iid"));
    }

    @Test
    @Ignore("Versioning of Configuration Versions should probably be done inside the repository ")
    public void incrementsConfigurationVersion() throws Exception {
        ArgumentCaptor<PluginConfiguration> lastPersistedConfig = ArgumentCaptor.forClass(PluginConfiguration.class);
        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture());

        assertEquals(1l, lastPersistedConfig.getValue().getVersion());

        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture());

        assertEquals(2l, lastPersistedConfig.getValue().getVersion());
    }

}
