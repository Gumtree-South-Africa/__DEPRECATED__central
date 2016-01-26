package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pragone
 * Created on 18/10/15 at 6:50 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public class CassandraIndexerClockRepository implements IndexerClockRepository {
    private final String cassandraDataCenter;
    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;
    private final Map<Statements, PreparedStatement> preparedStatements;

    private final Timer setTimer = TimingReports.newTimer("cassandra.indexerClockRepo-set");
    private final Timer clearTimer = TimingReports.newTimer("cassandra.indexerClockRepo-clear");
    private final Timer getTimer = TimingReports.newTimer("cassandra.indexerClockRepo-get");

    public CassandraIndexerClockRepository(String cassandraDataCenter, Session session) {
        // We should only index ES when we know the cluster is receiving all updates. When the other DC can't
        // write its changes to the local DC, it probably also can't be reached.
        // By using EACH_QUORUM for read consistency, queries fail when the other DC is not reachable.

        // CASSANDRA-9602: EACH_QUORUM for read is not supported in cassandra 2.1 and we are running in one DC for now so LOCAL_QUORUM is fine.
        this(cassandraDataCenter, session, ConsistencyLevel.LOCAL_QUORUM, ConsistencyLevel.LOCAL_QUORUM);
    }

    public static CassandraIndexerClockRepository createCassandraIndexerClockRepositoryForTesting(String cassandraDataCenter, Session session) {
        // During tests we have any one node. Use ONE as read/write consistency:
        return new CassandraIndexerClockRepository(cassandraDataCenter, session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    private CassandraIndexerClockRepository(String cassandraDataCenter, Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.cassandraDataCenter = cassandraDataCenter;
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
    }

    @Override
    public void set(DateTime lastRun) {
        try (Timer.Context ignored = setTimer.time()) {
            session.execute(Statements.UPDATE.bind(this, lastRun.toDate(), cassandraDataCenter));
        }
    }

    @Override
    public void clear() {
        try (Timer.Context ignored = clearTimer.time()) {
            session.execute(Statements.DELETE.bind(this, cassandraDataCenter));
        }
    }

    @Override
    public DateTime get() {
        try (Timer.Context ignored = getTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT.bind(this, cassandraDataCenter));
            Row row = result.one();
            if (row == null) {
                return null;
            }
            return new DateTime(row.getDate("lastindex").getTime());
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {
        SELECT("SELECT lastindex FROM core_indexer_clock WHERE datacenter=?"),
        UPDATE("UPDATE core_indexer_clock SET lastindex=? WHERE datacenter=?", true),
        DELETE("DELETE FROM core_indexer_clock WHERE datacenter=?", true);

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

        public Statement bind(CassandraIndexerClockRepository repository, Object... values) {
            return repository.preparedStatements.get(this).bind(values).
                    setConsistencyLevel(getConsistency(repository)).
                    setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistency(CassandraIndexerClockRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
