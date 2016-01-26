package com.ecg.replyts.core.runtime.persistence.mail;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.util.StreamUtils;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by pragone
 * Created on 15/10/15 at 10:22 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public class CassandraMailRepository extends AbstractMailRepository {

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer persistTimer = TimingReports.newTimer("cassandra.mailRepo-persist");
    private final Timer loadTimer = TimingReports.newTimer("cassandra.mailRepo-load");
    private final Timer createdBeforeTimer = TimingReports.newTimer("cassandra.mailRepo-findCreatedBefore");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.mailRepo-delete");
    private final Timer deleteMailsOlderThenTimer = TimingReports.newTimer("cassandra.mailRepo-deleteOlderThen");

    public CassandraMailRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
    }

    @Override
    protected void doPersist(String messageId, byte[] compress) {
        try (Timer.Context ignored = persistTimer.time()) {
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.INSERT_MAIL.bind(this, messageId, ByteBuffer.wrap(compress)));
            batch.add(Statements.INSERT_MAIL_CREATION_IDX.bind(this, messageId, new Date()));
            session.execute(batch);
        }
    }

    @Override
    protected Optional<byte[]> doLoad(String messageId) {
        try (Timer.Context ignored = loadTimer.time()) {
            Statement bound = Statements.SELECT_BY_MAIL_ID.bind(this, messageId);
            ResultSet resultset = session.execute(bound);
            return Optional
                    .ofNullable(resultset.one())
                    .map(row -> row.getBytes("mail").array());
        }
    }

    @Override
    public void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        try (Timer.Context ignored = deleteMailsOlderThenTimer.time()) {
            getMailIdsOlderThan(time, maxResults).forEach(this::deleteMail);
        }
    }

    protected Stream<String> getMailIdsOlderThan(DateTime time, int maxResults) {
        try (Timer.Context ignored = createdBeforeTimer.time()) {
            Statement bound = Statements.SELECT_MAIL_ID_WHERE_MAIL_CREATION_OLDER_THAN.bind(this, time.toDate(), maxResults);
            ResultSet resultset = session.execute(bound);
            return StreamUtils.toStream(resultset).map(row -> row.getString("mail_id"));
        }
    }

    @Override
    public void deleteMail(String messageId) {
        try (Timer.Context ignored = deleteTimer.time()) {
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.DELETE_BY_ID.bind(this, messageId));
            batch.add(Statements.DELETE_CREATION_IDX_BY_ID.bind(this, messageId));
            session.execute(batch);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    enum Statements {
        INSERT_MAIL("INSERT INTO core_mail (mail_id, mail) VALUES (?,?)", true),
        SELECT_BY_MAIL_ID("SELECT mail FROM core_mail WHERE mail_id=?"),
        DELETE_BY_ID("DELETE FROM core_mail WHERE mail_id=?", true),
        INSERT_MAIL_CREATION_IDX("INSERT INTO core_mail_creation_idx (mail_id, mail_creation_date) VALUES (?,?)", true),
        SELECT_MAIL_ID_WHERE_MAIL_CREATION_OLDER_THAN("SELECT mail_id FROM core_mail_creation_idx WHERE mail_creation_date <= ? LIMIT ? ALLOW FILTERING"),
        DELETE_CREATION_IDX_BY_ID("DELETE FROM core_mail_creation_idx WHERE mail_id=?", true);

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
            Map<Statements, PreparedStatement> statements = new EnumMap<>(Statements.class);
            for (Statements statement : values()) {
                statements.put(statement, session.prepare(statement.cql));
            }
            return ImmutableMap.copyOf(statements);
        }

        public Statement bind(CassandraMailRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraMailRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
