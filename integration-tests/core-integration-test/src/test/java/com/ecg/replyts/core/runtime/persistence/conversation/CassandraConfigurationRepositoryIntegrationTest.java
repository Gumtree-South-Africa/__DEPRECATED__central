package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.integration.cassandra.CassandraRunner;
import com.ecg.replyts.util.CassandraTestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
@CassandraDataSet(
        keyspace = "replyts2_configuration_test",
        value = {"cassandra_schema.cql"}
)
@TestExecutionListeners(CassandraRunner.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CassandraConfigurationRepositoryIntegrationTest {

    public static final String KEYSPACE = "replyts2_configuration_test";

    private static Session session;
    private static CassandraConfigurationRepository configurationRepository;

    @BeforeClass
    public static void init() {
        session = CassandraTestUtil.newSession(KEYSPACE);
        configurationRepository = new CassandraConfigurationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanTable() {
        CassandraTestUtil.cleanTables(session, KEYSPACE);
    }

    @Test
    public void shouldInsertAndReadConfiguration() {
        ConfigurationId id = new ConfigurationId(DummyFactory.class, "id1");
        PluginConfiguration configuration = new PluginConfiguration(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration);

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
        ConfigurationId id = new ConfigurationId(DummyFactory.class, "id1");
        PluginConfiguration configuration = new PluginConfiguration(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration);

        ConfigurationId sameId = new ConfigurationId(DummyFactory.class, "id1");
        PluginConfiguration configuration2 = new PluginConfiguration(sameId, 2, PluginState.DISABLED, 2, new TextNode("other_json"));
        configurationRepository.persistConfiguration(configuration2);

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
        ConfigurationId id = new ConfigurationId(DummyFactory.class, "id1");
        PluginConfiguration configuration = new PluginConfiguration(id, 1, PluginState.ENABLED, 1, new TextNode("some_json"));
        configurationRepository.persistConfiguration(configuration);

        List<PluginConfiguration> configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 1);
        configurationRepository.deleteConfiguration(new ConfigurationId(DummyFactory.class, "id1"));
        configurations = configurationRepository.getConfigurations();

        assertEquals(configurations.size(), 0);
    }

    class DummyFactory implements BasePluginFactory<String> {
        @Override
        public String createPlugin(String instanceName, JsonNode configuration) {
            return null;
        }
    }
}