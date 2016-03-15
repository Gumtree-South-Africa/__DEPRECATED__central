package nl.marktplaats.filter.volume;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import nl.marktplaats.filter.volume.persistence.CassandraVolumeFilterEventRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class CassandraVolumeFilterEventRepositoryIntegrationTest {

    public static final String KEYSPACE = "volume_filter_test";

    private static EmbeddedCassandra casdb;
    private static Session session;
    private static CassandraVolumeFilterEventRepository volumeFilterEventRepository;
    private static final int TTL = 100;

    @BeforeClass
    public static void init() {
        casdb = EmbeddedCassandra.getInstance();
        session = casdb.loadSchema(KEYSPACE, "cassandra_schema.cql", "cassandra_volume_filter_schema.cql");
        volumeFilterEventRepository = new CassandraVolumeFilterEventRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanup() {
        casdb.cleanTables(session, KEYSPACE);
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldStoreEvents() {
        volumeFilterEventRepository.record("u1@mail.com", TTL);
        volumeFilterEventRepository.record("u99@mail.com", TTL);
        volumeFilterEventRepository.record("u1@mail.com", TTL);
        volumeFilterEventRepository.record("u99@mail.com", TTL);
        long count = session.execute("SELECT count(*) from volume_events where user_id = 'u1@mail.com'").one().getLong(0);
        assertThat(count, is(2L));
    }

    @Test
    public void shouldCountEvents() {
        volumeFilterEventRepository.record("u3@mail.com", TTL);
        assertThat(volumeFilterEventRepository.count("u3@mail.com", 5), is(1));
        DateTimeUtils.setCurrentMillisFixed(new DateTime().plusSeconds(6).getMillis()); //set current time to a time in the future
        assertThat(volumeFilterEventRepository.count("u3@mail.com", 1), is(0));
    }

    @Test
    public void shouldExpireVolumeEventWithTTL() throws InterruptedException {
        volumeFilterEventRepository.record("u2@mail.com", 1);
        Thread.sleep(2000);
        long count = session.execute("SELECT count(*) from volume_events where user_id = 'u2@mail.com'").one().getLong(0);
        assertThat(count, is(0L));
    }

}
