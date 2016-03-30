package com.ecg.messagecenter.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import org.junit.After;

public class CassandraPostBoxRepositoryIntegrationTest extends AbstractPostBoxRepositoryTest<CassandraPostBoxRepository> {
    private final String KEYSPACE = EmbeddedCassandra.createUniqueKeyspaceName("p_");

    private EmbeddedCassandra casdb = EmbeddedCassandra.getInstance();

    private Session session;

    @After
    public void cleanCassandra() {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Override
    protected CassandraPostBoxRepository createPostBoxRepository() throws Exception {
        session = casdb.loadSchema(KEYSPACE, "cassandra_schema.cql", "cassandra_messagebox_schema.cql");
        CassandraPostBoxRepository r = new CassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        r.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
        return r;
    }
}