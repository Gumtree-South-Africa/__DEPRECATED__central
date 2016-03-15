package com.ecg.replyts.core.runtime.indexer;

import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import org.junit.After;
import org.junit.BeforeClass;

public class CassandraIndexerClockRepositoryIntegrationTest extends AbstractIndexerClockRepositoryTest<CassandraIndexerClockRepository> {

    private static final String KEYSPACE = "replyts2_clock_test";
    private static EmbeddedCassandra casdb;
    private static Session session;

    @Override
    protected CassandraIndexerClockRepository createClockRepository() throws Exception {
        return CassandraIndexerClockRepository.createCassandraIndexerClockRepositoryForTesting("datacenter1", session);
    }

    @BeforeClass
    public static void init() {
        casdb = EmbeddedCassandra.getInstance();
        session = casdb.initStdSchema(KEYSPACE);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }

}
