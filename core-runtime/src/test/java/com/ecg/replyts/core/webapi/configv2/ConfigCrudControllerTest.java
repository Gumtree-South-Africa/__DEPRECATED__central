package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfigCrudControllerTest {
    @InjectMocks
    private ConfigCrudController configCrudController;

    @Mock
    private ConfigurationRepository repository;

    @Mock
    private ConfigurationUpdateNotifier updateNotifier;

    private JsonNode validPutData;

    @Before
    public void setup() throws Exception {
        when(updateNotifier.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenReturn(true);

        validPutData = JsonObjects.builder().attr("configuration", JsonObjects.newJsonObject()).build();
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForEmptyInstanceId() throws Exception {
        configCrudController.addConfiguration(FilterFactory.class.getName(), null, JsonObjects.newJsonObject());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForNonexistentPluginFactory() throws Exception {
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
    public void deletesContent() throws Exception {
        configCrudController.deleteConfiguration(FilterFactory.class.getName(), "iid");
        verify(repository).deleteConfiguration(new ConfigurationId(FilterFactory.class, "iid"));
    }

    @Test
    @Ignore("Versioning of Configuration Versions should probably be done inside the repository ")
    public void incrementsConfigurationVersion() throws Exception {
        ArgumentCaptor<PluginConfiguration> lastPersistedConfig = ArgumentCaptor.forClass(PluginConfiguration.class);
        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture());

        assertEquals(1L, lastPersistedConfig.getValue().getVersion());

        configCrudController.addConfiguration(FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture());

        assertEquals(2L, lastPersistedConfig.getValue().getVersion());
    }

}
