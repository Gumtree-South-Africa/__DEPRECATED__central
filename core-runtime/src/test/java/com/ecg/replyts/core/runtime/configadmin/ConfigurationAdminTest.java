package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConfigurationAdminTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigurationAdminTest {
    @Autowired
    private ClusterRefreshPublisher clusterRefreshPublisher;

    @Autowired
    private ConfigurationAdmin<String> admin;

    public static class Factory1 implements BasePluginFactory<String> {
        @Nonnull
        public String createPlugin(String instanceName, JsonNode configuration) {
            return "Factory1" + configuration;
        }
    }

    public static class Factory2 implements BasePluginFactory<String> {
        @Nonnull
        public String createPlugin(String instanceName, JsonNode configuration) {
            return "Factory2" + configuration;
        }
    }

    @Test
    public void createsNewConfiguration() throws Exception {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);

        assertEquals("Factory1{}", admin.getRunningServices().get(0).getCreatedService());
    }

    @Test
    public void overridesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        PluginConfiguration newConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(newConf);

        assertEquals("Factory1{}", admin.getRunningServices().get(0).getCreatedService());
    }

    @Test
    public void mergesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        PluginConfiguration newConf = new PluginConfiguration(new ConfigurationId(Factory2.class, "inst1"), 200, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(newConf);

        assertEquals("Factory2{}", admin.getRunningServices().get(0).getCreatedService());
        assertEquals("Factory1{}", admin.getRunningServices().get(1).getCreatedService());
    }

    @Test
    public void removesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1L, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        admin.deleteConfiguration(baseConf.getId());
        assertTrue(admin.getRunningServices().isEmpty());
    }

    @Test
    public void informsBusOnDeleteConfiguration() {
        admin.deleteConfiguration(new ConfigurationId(Factory1.class, "inst1"));
        verify(clusterRefreshPublisher).publish();
    }

    @Configuration
    static class TestContext {
        @MockBean
        private ClusterRefreshPublisher clusterRefreshPublisher;

        @Bean
        public List<BasePluginFactory<String>> factories() {
            return Arrays.asList(new Factory1(), new Factory2());
        }

        @Bean
        public ConfigurationAdmin<String> configurationAdmin(List<BasePluginFactory<String>> factories) {
            return new ConfigurationAdmin<>(factories, "");
        }
    }
}