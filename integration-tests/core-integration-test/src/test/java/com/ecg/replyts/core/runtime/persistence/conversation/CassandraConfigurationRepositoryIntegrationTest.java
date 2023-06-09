package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CassandraConfigurationRepositoryIntegrationTest {
    private String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private Session session;
    private CassandraConfigurationRepository configurationRepository;
    private CassandraIntegrationTestProvisioner casdb;

    @Before
    public void init() {
        casdb = CassandraIntegrationTestProvisioner.getInstance();
        session = casdb.initStdSchema(KEYSPACE);
        configurationRepository = new CassandraConfigurationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanTable()  {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Test
    public void shouldInsertAndReadConfiguration() {
        ConfigurationId id = new ConfigurationId(DummyFactory.IDENTIFIER, "id1");
        PluginConfiguration configuration = PluginConfiguration.createWithRandomUuid(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration, "127.0.0.1");

        List<PluginConfiguration> configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 1);
        PluginConfiguration pluginConfiguration = configurations.get(0);

        assertEquals(pluginConfiguration.getId().getInstanceId(), "id1");
        assertEquals(pluginConfiguration.getPriority(), 1);
        assertEquals(pluginConfiguration.getState(), PluginState.ENABLED);
        assertEquals(pluginConfiguration.getConfiguration().asText(), "some_json");
    }

    @Test
    public void shouldUpdateConfiguration() {
        ConfigurationId id = new ConfigurationId(DummyFactory.IDENTIFIER, "id1");
        PluginConfiguration configuration = PluginConfiguration.createWithRandomUuid(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration, "127.0.0.1");

        ConfigurationId sameId = new ConfigurationId(DummyFactory.IDENTIFIER, "id1");
        PluginConfiguration configuration2 = PluginConfiguration.createWithRandomUuid(sameId, 2, PluginState.DISABLED, 2, new TextNode("other_json"));
        configurationRepository.persistConfiguration(configuration2, "127.0.0.1");

        List<PluginConfiguration> configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 1);
        PluginConfiguration pluginConfiguration = configurations.get(0);

        assertEquals(pluginConfiguration.getId().getInstanceId(), "id1");
        assertEquals(pluginConfiguration.getPriority(), 2);
        assertEquals(pluginConfiguration.getState(), PluginState.DISABLED);
        assertEquals(pluginConfiguration.getConfiguration().asText(), "other_json");
    }


    @Test
    public void shouldDeleteConfiguration() {
        ConfigurationId id = new ConfigurationId(DummyFactory.IDENTIFIER, "id1");
        PluginConfiguration configuration = PluginConfiguration.createWithRandomUuid(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration, "127.0.0.1");

        List<PluginConfiguration> configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 1);
        configurationRepository.deleteConfiguration(DummyFactory.IDENTIFIER, "id1", "127.0.0.1");
        configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 0);
    }

    class DummyFactory implements BasePluginFactory<String> {

        public static final String IDENTIFIER = "com.ecg.replyts.core.runtime.persistence.conversation.DummyFactory";

        @Override
        public String createPlugin(String instanceName, JsonNode configuration) {
            return null;
        }

        @Override
        public String getIdentifier() {
            return IDENTIFIER;
        }
    }
}