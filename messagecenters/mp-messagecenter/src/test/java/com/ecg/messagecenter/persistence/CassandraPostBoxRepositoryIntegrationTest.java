package com.ecg.messagecenter.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import com.ecg.replyts.util.CassandraTestUtil;
import org.junit.After;
import org.junit.BeforeClass;

public class CassandraPostBoxRepositoryIntegrationTest extends AbstractPostBoxRepositoryTest<CassandraPostBoxRepository> {

    private static final String KEYSPACE = "messagebox_test";
    private static EmbeddedCassandra c;

    @BeforeClass
    public static void init() throws Exception {
        c = new EmbeddedCassandra(KEYSPACE);
    }

    @After
    public void cleanCassandra() {
        c.cleanEmbeddedCassandra();
    }

    @Override
    protected CassandraPostBoxRepository createPostBoxRepository() throws Exception {
        c.start("/cassandra_messagebox_schema.cql");
        Session session = CassandraTestUtil.newSession(KEYSPACE);
        CassandraPostBoxRepository r = new CassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        r.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
        return r;
    }
}