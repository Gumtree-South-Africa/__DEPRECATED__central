package com.ecg.messagecenter.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.util.CassandraTestUtil;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@EmbeddedCassandra
@CassandraDataSet(keyspace = CassandraPostBoxRepositoryIntegrationTest.KEYSPACE, value = {"cassandra_messagebox_schema.cql"})
@TestExecutionListeners(CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CassandraPostBoxRepositoryIntegrationTest extends AbstractPostBoxRepositoryTest<CassandraPostBoxRepository> {

    // Notes:
    // CassandraUnitDependencyInjectionIntegrationTestExecutionListener cleans the database after each test.

    public static final String KEYSPACE = "messagebox_test";
    private static CassandraPostBoxRepository cassandraPostBoxRepository;

    @BeforeClass
    public static void init() {
        Session session = CassandraTestUtil.newSession(KEYSPACE);
        cassandraPostBoxRepository = new CassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        cassandraPostBoxRepository.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
    }

    @Override
    protected CassandraPostBoxRepository createPostBoxRepository() {
        return cassandraPostBoxRepository;
    }

}
