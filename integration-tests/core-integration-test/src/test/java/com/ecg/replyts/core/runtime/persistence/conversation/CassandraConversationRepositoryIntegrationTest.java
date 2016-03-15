package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

public class CassandraConversationRepositoryIntegrationTest extends ConversationRepositoryIntegrationTestBase<CassandraConversationRepository> {

    private static final String KEYSPACE = "replyts2_conversation_test";
    private static Session session;
    private static final EmbeddedCassandra casdb = EmbeddedCassandra.getInstance();

    @BeforeClass
    public static void init() {
        session = casdb.initStdSchema(KEYSPACE);
    }

    @Override
    protected CassandraConversationRepository createConversationRepository() {
        CassandraConversationRepository myRepo = new CassandraConversationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        myRepo.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
        return myRepo;
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session,KEYSPACE);
    }
}
