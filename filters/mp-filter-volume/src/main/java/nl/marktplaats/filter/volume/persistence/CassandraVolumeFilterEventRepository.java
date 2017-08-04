package nl.marktplaats.filter.volume.persistence;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * +----------------+----------+
 * | user_id        | text     |  (partitionkey)
 * | received_time  | timeuuid |  (clustering key)
 * +----------------+----------+
 */
public class CassandraVolumeFilterEventRepository implements VolumeFilterEventRepository {

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer recordTimer = TimingReports.newTimer("cassandra.volumeFilterRepo.record");
    private final Timer countTimer = TimingReports.newTimer("cassandra.volumeFilterRepo.count");


    public CassandraVolumeFilterEventRepository(Session session,
                                                ConsistencyLevel readConsistency,
                                                ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
    }

    public void record(String userId, int ttlInSeconds) {
        try (Timer.Context ignored = recordTimer.time()) {
            session.execute(Statements.INSERT.bind(this, userId, ttlInSeconds));
        }
    }

    public int count(String userId, int maxAgeInSeconds) {
        try (Timer.Context ignored = countTimer.time()) {
            Date after = new DateTime().minusSeconds(maxAgeInSeconds).toDate();
            return (int) session.execute(Statements.COUNT.bind(this, userId, after)).one().getLong(0);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {
        COUNT("SELECT count(*) FROM volume_events WHERE user_id=? AND received_time > maxTimeuuid(?)"),
        INSERT("INSERT INTO volume_events (user_id, received_time) VALUES (?, now()) USING TTL ?", true);

        private final String cql;
        private final boolean modifying;

        Statements(String cql) {
            this(cql, false);
        }

        Statements(String cql, boolean modifying) {
            this.cql = cql;
            this.modifying = modifying;
        }

        public static Map<Statements, PreparedStatement> prepare(Session session) {
            Map<Statements, PreparedStatement> result = new EnumMap<>(Statements.class);
            for (Statements statement : values()) {
                result.put(statement, session.prepare(statement.cql));
            }
            return result;
        }

        public Statement bind(CassandraVolumeFilterEventRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
                    .setIdempotent(!modifying);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraVolumeFilterEventRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
