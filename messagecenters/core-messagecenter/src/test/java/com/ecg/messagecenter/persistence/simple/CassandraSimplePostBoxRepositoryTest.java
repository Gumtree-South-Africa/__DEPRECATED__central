package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { CassandraSimplePostBoxRepositoryTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = cassandra",
  "replyts.maxConversationAgeDays = 25"
})
public class CassandraSimplePostBoxRepositoryTest {
    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Test
    public void persistsPostbox() {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<AbstractConversationThread>emptyList(), 25);

        postBoxRepository.write(box);

        assertNotNull(postBoxRepository.byId("foo@bar.com"));
    }

    @Test
    public void cleansUpPostbox() {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Arrays.asList(createConversationThread(now(), "123")), 25);

        postBoxRepository.write(box);

        postBoxRepository.cleanup(now());

        assertEquals("PostBox contains no conversation threads anymore", 0, postBoxRepository.byId("foo@bar.com").getConversationThreads().size());
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Arrays.asList(createConversationThread(now(), "123")), 25);

        postBoxRepository.write(box);

        postBoxRepository.cleanup(now().minusHours(1));

        assertEquals("PostBox still contains its 1 conversation thread", 1, postBoxRepository.byId("foo@bar.com").getConversationThreads().size());
    }

    private AbstractConversationThread createConversationThread(DateTime date, String conversationId) {
        return new PostBoxTest.ConversationThread("123", conversationId, date, date, date, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Configuration
    @Import({
      CassandraSimplePostBoxConfiguration.class,
      JacksonAwareObjectMapperConfigurer.class
    })
    static class TestContext {
        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Bean
        public Session cassandraSession() {
            String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
            String[] schemas = new String[] { "cassandra_schema.cql", "cassandra_messagecenter_schema.cql" };

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