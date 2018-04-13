package com.ecg.comaas.core.filter.volume.registry;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class CassandraOccurrenceRegistry implements OccurrenceRegistry {
    private final Session session;
    private final Duration defaultExpirationPeriod;
    private final Map<Statements, PreparedStatement> preparedStatements;

    public CassandraOccurrenceRegistry(Session session, Duration defaultExpirationPeriod) {
        this.session = checkNotNull(session, "session");
        checkArgument(defaultExpirationPeriod.toMillis() > 0,
                "the expiration period must be a strictly positive number");
        this.defaultExpirationPeriod = checkNotNull(defaultExpirationPeriod, "defaultExpirationPeriod");
        this.preparedStatements = Statements.prepare(session);
    }

    @Override
    public void register(String userId, String messageId, Date receivedTime) {
        int ttl = (int) defaultExpirationPeriod.getSeconds();
        session.execute(Statements.UPSERT.bind(this, userId, receivedTime, messageId, ttl));
    }

    @Override
    public int count(String userId, Date fromTime) {
        return (int) session.execute(Statements.COUNT.bind(this, userId, fromTime)).one().getLong(0);
    }

    private enum Statements {
        COUNT("SELECT count(*) FROM message_occurrences WHERE user_id = ? AND received_time > ?"),
        UPSERT("INSERT INTO message_occurrences (user_id, received_time, message_id) VALUES (?, ?, ?) IF NOT EXISTS USING TTL ?", true);

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
            return Arrays.stream(values()).collect(toMap(identity(), s -> session.prepare(s.cql)));
        }

        public Statement bind(CassandraOccurrenceRegistry repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
                    .setIdempotent(!modifying);
        }
    }
}
