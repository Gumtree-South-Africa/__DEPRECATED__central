package com.ecg.replyts.core.runtime.indexer;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.ecg.replyts.util.CassandraTestUtil;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
@CassandraDataSet(keyspace = "replyts2_clock_test", value = {"cassandra_schema.cql"})
@TestExecutionListeners(CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CassandraIndexerClockRepositoryIntegrationTest extends AbstractIndexerClockRepositoryTest<CassandraIndexerClockRepository> {

    private static final String KEYSPACE = "replyts2_clock_test";
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
    protected CassandraIndexerClockRepository createClockRepository() throws Exception {
        return CassandraIndexerClockRepository.createCassandraIndexerClockRepositoryForTesting("datacenter1", session);
    }

    @After
    public void cleanupTables() {
        CassandraTestUtil.cleanTables(session, KEYSPACE);
    }
}
