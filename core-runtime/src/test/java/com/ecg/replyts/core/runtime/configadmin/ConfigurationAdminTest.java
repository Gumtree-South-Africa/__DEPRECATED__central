package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class ConfigurationAdminTest {

    @Mock
    private ClusterRefreshPublisher clusterRefreshPublisher;

    public static class Factory1 implements BasePluginFactory<String> {
        public String createPlugin(String instanceName, JsonNode configuration) {
            return "Factory1" + configuration;
        }
    }

    public static class Factory2 implements BasePluginFactory<String> {
        public String createPlugin(String instanceName, JsonNode configuration) {
            return "Factory2" + configuration;
        }
    }

    ConfigurationAdmin<String> admin;

    @Before
    public void setUp() throws Exception {
        admin = new ConfigurationAdmin<String>(Arrays.asList(new Factory1(), new Factory2()), "", clusterRefreshPublisher);
    }

    @Test
    public void createsNewConfiguration() throws Exception {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);

        Assert.assertEquals("Factory1{}", admin.getRunningServices().get(0).getCreatedService());
    }

    @Test
    public void overridesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        PluginConfiguration newConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(newConf);

        Assert.assertEquals("Factory1{}", admin.getRunningServices().get(0).getCreatedService());
    }

    @Test
    public void mergesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        PluginConfiguration newConf = new PluginConfiguration(new ConfigurationId(Factory2.class, "inst1"), 200, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(newConf);

        Assert.assertEquals("Factory2{}", admin.getRunningServices().get(0).getCreatedService());
        Assert.assertEquals("Factory1{}", admin.getRunningServices().get(1).getCreatedService());
    }

    @Test
    public void removesConfiguration() {
        PluginConfiguration baseConf = new PluginConfiguration(new ConfigurationId(Factory1.class, "inst1"), 100, PluginState.ENABLED, 1l, JsonObjects.newJsonObject());
        admin.putConfiguration(baseConf);
        admin.deleteConfiguration(baseConf.getId());
        Assert.assertTrue(admin.getRunningServices().isEmpty());
    }

    @Test
    public void informsBusOnDeleteConfiguration() {
        admin.deleteConfiguration(new ConfigurationId(Factory1.class, "inst1"));
        verify(clusterRefreshPublisher).publish();
    }
}
