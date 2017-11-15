package com.ecg.messagecenter.persistence.block;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CassandraConversationBlockRepositoryTest.TestContext.class})
@TestPropertySource(properties = {
        "persistence.strategy = cassandra",
        "replyts.maxConversationAgeDays = 25"
})
public class CassandraConversationBlockRepositoryTest {
    @Autowired
    private ConversationBlockRepository conversationBlockRepository;

    // Apply @DirtiesContext here so that the created ConversationBlock doesn't interfere with the other tests (will
    // trigger the creation of a new context and thus a new keyspace)

    @Test
    @DirtiesContext
    public void persistsPostbox() {
        ConversationBlock block = new ConversationBlock("123", 0, Optional.empty(), Optional.empty());

        conversationBlockRepository.write(block);

        assertNotNull(conversationBlockRepository.byId("123"));
    }

    @Test
    public void cleansUpPostbox() {
        ConversationBlock block = new ConversationBlock("123", 0, Optional.empty(), Optional.empty());

        conversationBlockRepository.write(block);

        conversationBlockRepository.cleanup(now().plusSeconds(1));

        assertNull(conversationBlockRepository.byId("123"));
    }

    @Test
    @DirtiesContext
    public void keepsPostboxEntriesThatAreTooNew() {
        ConversationBlock block = new ConversationBlock("123", 0, Optional.empty(), Optional.empty());

        conversationBlockRepository.write(block);

        conversationBlockRepository.cleanup(now().minusHours(1));

        assertNotNull("Repository still contains conversation thread", conversationBlockRepository.byId("123"));
    }

    @Configuration
    @Import({
            CassandraConversationBlockConfiguration.class,
            JacksonAwareObjectMapperConfigurer.class
    })
    static class TestContext {
        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Bean
        public Session cassandraSessionForMb() {
            String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
            String[] schemas = new String[]{"cassandra_schema.cql", "cassandra_kjca_messagecenter_schema.cql"};

            return CassandraIntegrationTestProvisioner.getInstance().loadSchema(keyspace, schemas);
        }

        @Bean(name = "cassandraReadConsistency")
        public ConsistencyLevel getCassandraReadConsistency() {
            return cassandraReadConsistency;
        }

        @Bean(name = "cassandraWriteConsistency")
        public ConsistencyLevel getCassandraWriteConsistency() {
            return cassandraWriteConsistency;
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
