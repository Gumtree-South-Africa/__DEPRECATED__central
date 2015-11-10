package nl.marktplaats.filter.volume;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.util.CassandraTestUtil;
import nl.marktplaats.filter.volume.persistence.CassandraVolumeFilterEventRepository;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")
@CassandraDataSet(keyspace = CassandraVolumeFilterEventRepositoryIntegrationTest.KEYSPACE, value = {"cassandra_volume_filter_schema.cql"})
@TestExecutionListeners(CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CassandraVolumeFilterEventRepositoryIntegrationTest {


    public static final String KEYSPACE = "volume_filter_test";

    private static Session session;
    private static CassandraVolumeFilterEventRepository volumeFilterEventRepository;

    @BeforeClass
    public static void init() {
        session = CassandraTestUtil.newSession(KEYSPACE);
        volumeFilterEventRepository = new CassandraVolumeFilterEventRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @Test
    public void shouldStoreEvents() {
        volumeFilterEventRepository.record("u1@mail.com", 10);
        volumeFilterEventRepository.record("u1@mail.com", 10);
        long count = session.execute("SELECT count(*) from volume_events where user_id = 'u1@mail.com'").one().getLong(0);
        assertThat(count, is(2L));
    }

    @Test
    public void shouldCountEvents() {
//        volumeFilterEventRepository.record("u1@mail.com", 10);
//        assertThat(volumeFilterEventRepository.count("u1@mail.com", 1), is(0));
    }

    @Test
    public void shouldExpireVolumeEventWithTTL() throws InterruptedException {
        volumeFilterEventRepository.record("u1@mail.com", 1);
        Thread.sleep(2000);
        long count = session.execute("SELECT count(*) from volume_events where user_id = 'u1@mail.com'").one().getLong(0);
        assertThat(count, is(0L));
    }

}
