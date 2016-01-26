package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ServiceReferenceTest {

    class ConfigurationTest implements BasePluginFactory<Object> {

        public Object createPlugin(String instanceName, JsonNode configuration) {
            return null;
        }

    }

    @Test
    public void doesAscendingOrder() throws Exception {
        PluginInstanceReference<Object> higherPrio = new PluginInstanceReference<Object>(new PluginConfiguration(new ConfigurationId(ConfigurationTest.class, "ins1"), 400, null, 1l, null), null);
        PluginInstanceReference<Object> lowerPrio = new PluginInstanceReference<Object>(new PluginConfiguration(new ConfigurationId(ConfigurationTest.class, "ins1"), 200, null, 1l, null), null);

        List<PluginInstanceReference<Object>> l = Arrays.asList(lowerPrio, higherPrio);
        Collections.sort(l, PluginInstanceReference.PLUGIN_REF_ORDERING_COMPARATOR);
        Assert.assertEquals(l.get(0), higherPrio);
    }
}
