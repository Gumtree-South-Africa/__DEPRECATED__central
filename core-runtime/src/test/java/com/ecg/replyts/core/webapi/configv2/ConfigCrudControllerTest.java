package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationValidator;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfigCrudControllerTest {

    private ConfigCrudController configCrudController;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ConfigurationRepository repository;

    @Mock
    private ConfigurationValidator configurationValidator;

    @Mock
    private ConfigurationPublisher configKafkaPublisher;

    @Mock
    private ClusterRefreshPublisher configHazelcastPublisher;

    private JsonNode validPutData;

    @Before
    public void setup() throws Exception {
        configCrudController = new ConfigCrudController(
                false,
                configKafkaPublisher,
                configHazelcastPublisher,
                configurationValidator,
                repository);

        when(configurationValidator.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        validPutData = JsonObjects.builder().attr("configuration", JsonObjects.newJsonObject()).build();
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForEmptyInstanceId() throws Exception {
        configCrudController.addConfiguration(request, FilterFactory.class.getName(), null, JsonObjects.newJsonObject());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForNonexistentPluginFactory() throws Exception {
        configCrudController.addConfiguration(request, "com.goo.bar", "foo", JsonObjects.newJsonObject());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsConfigForUnknownPluginFactory() throws Exception {
        when(configurationValidator.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenReturn(false);
        configCrudController.addConfiguration(request, FilterFactory.class.getName(), "i", validPutData);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidConfig() throws Exception {
        when(configurationValidator.validateConfiguration(Mockito.any(PluginConfiguration.class))).thenThrow(new IllegalStateException());
        configCrudController.addConfiguration(request, FilterFactory.class.getName(), "i", validPutData);
    }

    @Test
    public void persistsValidConfiguration() throws Exception {
        configCrudController.addConfiguration(request, FilterFactory.class.getName(), "i", validPutData);
        verify(repository).persistConfiguration(Mockito.any(PluginConfiguration.class), anyString());
    }

    @Test
    public void deletesContent() throws Exception {
        configCrudController.deleteConfiguration(request, FilterFactory.class.getName(), "iid");
        verify(repository).deleteConfiguration(FilterFactory.class.getName(), "iid", "127.0.0.1");
    }

    @Test
    @Ignore("Versioning of Configuration Versions should probably be done inside the repository ")
    public void incrementsConfigurationVersion() throws Exception {
        ArgumentCaptor<PluginConfiguration> lastPersistedConfig = ArgumentCaptor.forClass(PluginConfiguration.class);
        configCrudController.addConfiguration(request, FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture(), "127.0.0.1");

        assertEquals(1L, lastPersistedConfig.getValue().getVersion());

        configCrudController.addConfiguration(request, FilterFactory.class.getName(), "i", validPutData);
        verify(repository, atLeastOnce()).persistConfiguration(lastPersistedConfig.capture(), "127.0.0.1");

        assertEquals(2L, lastPersistedConfig.getValue().getVersion());
    }

}
