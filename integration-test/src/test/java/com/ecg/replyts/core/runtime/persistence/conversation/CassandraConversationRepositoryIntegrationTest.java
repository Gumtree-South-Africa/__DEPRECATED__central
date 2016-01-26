package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.util.CassandraTestUtil;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;

import static org.joda.time.DateTime.now;

@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
@CassandraDataSet(keyspace = "replyts2_conversation_test", value = {"cassandra_schema.cql"})
@TestExecutionListeners(CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CassandraConversationRepositoryIntegrationTest extends ConversationRepositoryIntegrationTestBase<CassandraConversationRepository> {

    private static final String KEYSPACE = "replyts2_conversation_test";
    private static Session session;

    @BeforeClass
    public static void init() {
        Cluster cluster = Cluster.builder()
                .addContactPoints(EmbeddedCassandraServerHelper.getHost())
                .withPort(EmbeddedCassandraServerHelper.getNativeTransportPort())
                .build();
        session = cluster.connect(KEYSPACE);
    }

    @Override
    protected CassandraConversationRepository createConversationRepository() {
        CassandraConversationRepository myRepo = new CassandraConversationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        myRepo.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
        return myRepo;
    }

    @After
    public void cleanupTables() {
        CassandraTestUtil.cleanTables(session, KEYSPACE);
    }
}
