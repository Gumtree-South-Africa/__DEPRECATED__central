package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.VClock;
import com.ecg.replyts.app.filterchain.FilterChainTest;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.persistence.GZip;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationConverterTest {
    private static final String BUCKET_NAME = "config";
    private ConfigurationConverter converter = new ConfigurationConverter(BUCKET_NAME);

    @Mock
    private IRiakObject obj;

    @Mock
    private VClock vClock;

    @Test
    public void returnsEmptyConfigurationsListOnNoKeyFound() {
        assertEquals(ImmutableList.<ConfigurationObject>of(), converter.toDomain(null).getConfigurationObjects());
    }

    @Test
    public void parsesExistingEmptyCofigurationsList() {
        when(obj.getValueAsString()).thenReturn("[]");
        assertEquals(ImmutableList.<ConfigurationObject>of(), converter.toDomain(obj).getConfigurationObjects());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsToStoreConfigurationIfTooBig() {
        converter = new ConfigurationConverter(ValueSizeConstraint.maxMb(0), BUCKET_NAME);
        converter.fromDomain(new Configurations(Lists.<ConfigurationObject>newArrayList(), true), vClock);
    }

    @Test
    public void keepUncompressedStatus() {
        String configs = createJsonConfigString();
        when(obj.getValueAsString()).thenReturn(configs);

        IRiakObject riakObject = converter.fromDomain(converter.toDomain(obj), vClock);
        Configurations configurations = converter.toDomain(riakObject);

        assertThat(configurations.isCompressed()).isFalse();
    }

    @Test
    public void keepCompressedStatus() {
        String configs = createJsonConfigString();
        when(obj.getContentType()).thenReturn("application/x-gzip");
        byte[] bytes = GZip.zip(configs.getBytes(Charsets.UTF_8));
        when(obj.getValue()).thenReturn(bytes);

        IRiakObject riakObject = converter.fromDomain(converter.toDomain(obj), vClock);
        Configurations configurations = converter.toDomain(riakObject);

        assertThat(configurations.isCompressed()).isTrue();
    }

    @Test
    public void readUncompressedConfiguration() {
        String configs = createJsonConfigString();
        when(obj.getValueAsString()).thenReturn(configs);

        Configurations configurations = converter.toDomain(obj);

        assertThat(configurations.isCompressed()).isFalse();
        assertConfigurationValues(configurations);
    }

    @Test
    public void readCompressedConfiguration() {
        String configs = createJsonConfigString();

        when(obj.getContentType()).thenReturn("application/x-gzip");

        byte[] bytes = GZip.zip(configs.getBytes(Charsets.UTF_8));
        when(obj.getValue()).thenReturn(bytes);

        Configurations configurations = converter.toDomain(obj);

        assertThat(configurations.isCompressed()).isTrue();
        assertConfigurationValues(configurations);
    }

    private void assertConfigurationValues(Configurations configurations) {
        List<ConfigurationObject> configurationObjects = configurations.getConfigurationObjects();
        assertThat(configurationObjects).hasSize(1);

        ConfigurationObject configurationObject = configurationObjects.get(0);
        assertThat(configurationObject.getTimestamp()).isEqualTo(120);

        PluginConfiguration config = configurationObject.getPluginConfiguration();
        assertThat(config.getId().getPluginFactory()).isEqualTo(FilterChainTest.ExampleFilterFactory.IDENTIFIER);
        assertThat(config.getPriority()).isEqualTo(10);
        assertThat(config.getVersion()).isEqualTo(123);
        assertThat(config.getId().getInstanceId()).isEqualTo("Sample");
        assertThat(config.getConfiguration().get("foo").asText()).isEqualTo("baar");
    }

    private String createJsonConfigString() {
        ObjectNode config = JsonObjects
                .builder()
                .attr("config", JsonObjects.builder().attr("foo", "baar"))
                .attr("pluginFactory", FilterChainTest.ExampleFilterFactory.IDENTIFIER)
                .attr("instanceId", "Sample")
                .attr("priority", 10)
                .attr("version", 123)
                .attr("state", PluginState.ENABLED.name())
                .attr("timestamp", 120).build();
        ArrayNode configs = JsonObjects.newJsonArray();
        configs.add(config);
        return configs.toString();
    }


    @Test
    public void marshallingAndUnmarshallingWorks() {
        ConfigurationId configId = new ConfigurationId(FilterChainTest.ExampleFilterFactory.IDENTIFIER, "fooInst");
        Configurations config = new Configurations(Lists.newArrayList(new ConfigurationObject(1L, new PluginConfiguration(
                configId,
                100,
                PluginState.ENABLED,
                123,
                JsonObjects.builder().attr("x", "y").build()
        ))), true);
        Configurations configurations = converter.toDomain(converter.fromDomain(config, null));

        assertEquals(1, configurations.getConfigurationObjects().size());
        PluginConfiguration pc = configurations.getConfigurationObjects().get(0).getPluginConfiguration();
        assertEquals(configId, pc.getId());
        assertEquals(100, pc.getPriority());
        assertEquals(PluginState.ENABLED, pc.getState());
        // version does not need to be equal after update
        assertEquals("y", pc.getConfiguration().get("x").asText());
    }

    @Test(expected = RuntimeException.class)
    public void rejectsNonMapFromStorage() {
        when(obj.getValueAsString()).thenReturn("{}");
        converter.toDomain(obj);
    }


}
