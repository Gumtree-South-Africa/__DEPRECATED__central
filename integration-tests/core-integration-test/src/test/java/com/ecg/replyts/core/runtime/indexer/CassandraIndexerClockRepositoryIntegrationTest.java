package com.ecg.replyts.core.runtime.indexer;

import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public class CassandraIndexerClockRepositoryIntegrationTest extends AbstractIndexerClockRepositoryTest<CassandraIndexerClockRepository> {
    private final String KEYSPACE = EmbeddedCassandra.createUniqueKeyspaceName();

    private EmbeddedCassandra casdb = EmbeddedCassandra.getInstance();

    private Session session;

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
