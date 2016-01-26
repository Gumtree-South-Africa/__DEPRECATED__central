package com.ecg.replyts.core.runtime.persistence.config;

import com.ecg.replyts.app.filterchain.FilterChainTest;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationsTest {

    private Configurations configurations;

    @Mock
    private PluginConfiguration existingConfig1;

    @Mock
    private PluginConfiguration existingConfig2;


    @Before
    public void setUp() {
        when(existingConfig1.getId()).thenReturn(new ConfigurationId(FilterChainTest.ExampleFilterFactory.class, "existing1"));
        when(existingConfig2.getId()).thenReturn(new ConfigurationId(FilterChainTest.ExampleFilterFactory.class, "existing2"));
        configurations = new Configurations(Lists.newArrayList(new ConfigurationObject(10, existingConfig1), new ConfigurationObject(20, existingConfig2)), true);
    }

    @Test
    public void removesElementFromListCorrectly() {
        Configurations newConfig = configurations.delete(new ConfigurationId(FilterChainTest.ExampleFilterFactory.class, "existing1"));

        assertEquals(1, newConfig.getConfigurationObjects().size());
        assertEquals("existing2", newConfig.getConfigurationObjects().get(0).getPluginConfiguration().getId().getInstanceId());
    }

    @Test
    public void updatesElementCorrectly() {

        PluginConfiguration pluginConf = mock(PluginConfiguration.class);
        when(pluginConf.getId()).thenReturn(new ConfigurationId(FilterChainTest.ExampleFilterFactory.class, "existing2"));
        ConfigurationObject toUpdate = new ConfigurationObject(222, pluginConf);

        Configurations newConfig = configurations.addOrUpdate(toUpdate);
        List<ConfigurationObject> configurationObjects = newConfig.getConfigurationObjects();

        assertEquals(2, configurationObjects.size());
        assertEquals(10, configurationObjects.get(0).getTimestamp());
        assertEquals(222, configurationObjects.get(1).getTimestamp());
    }

    @Test
    public void addsElementCorrectly() {

        PluginConfiguration pluginConf = mock(PluginConfiguration.class);
        when(pluginConf.getId()).thenReturn(new ConfigurationId(FilterChainTest.ExampleFilterFactory.class, "new1"));
        ConfigurationObject toUpdate = new ConfigurationObject(222, pluginConf);

        Configurations newConfig = configurations.addOrUpdate(toUpdate);
        List<ConfigurationObject> configurationObjects = newConfig.getConfigurationObjects();

        assertEquals(3, configurationObjects.size());
        assertEquals(222, configurationObjects.get(2).getTimestamp());
    }
}
