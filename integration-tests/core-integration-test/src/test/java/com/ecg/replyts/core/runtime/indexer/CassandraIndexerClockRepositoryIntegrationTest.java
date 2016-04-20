package com.ecg.replyts.core.runtime.indexer;

import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.After;

public class CassandraIndexerClockRepositoryIntegrationTest extends AbstractIndexerClockRepositoryTest<CassandraIndexerClockRepository> {

    private String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
    private CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();
    private Session session = null;

    public void init() {
        if (session == null) {
            session = casdb.initStdSchema(KEYSPACE);
        }
    }

    @Override
    protected CassandraIndexerClockRepository createClockRepository() throws Exception {
        init();
        return CassandraIndexerClockRepository.createCassandraIndexerClockRepositoryForTesting("datacenter1", session);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }
}
