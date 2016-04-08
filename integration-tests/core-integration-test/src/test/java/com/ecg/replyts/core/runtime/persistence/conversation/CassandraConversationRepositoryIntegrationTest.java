package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.After;

public class CassandraConversationRepositoryIntegrationTest extends ConversationRepositoryIntegrationTestBase<CassandraConversationRepository> {
    private String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private Session session = null;

    private CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    public void init() {
        if (session == null) {
            session = casdb.initStdSchema(KEYSPACE);
        }
    }

    @Override
    protected CassandraConversationRepository createConversationRepository() {
        init();

        CassandraConversationRepository myRepo = new CassandraConversationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        myRepo.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());

        return myRepo;
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }
}
