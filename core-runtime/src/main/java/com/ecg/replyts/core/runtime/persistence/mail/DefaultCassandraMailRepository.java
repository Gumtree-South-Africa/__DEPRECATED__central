package com.ecg.replyts.core.runtime.persistence.mail;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.api.model.mail.MailCreationDate;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.util.StreamUtils;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

/**
 * Created by pragone
 * Created on 15/10/15 at 10:22 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public class DefaultCassandraMailRepository extends AbstractMailRepository implements CassandraMailRepository {

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer persistTimer = TimingReports.newTimer("cassandra.mailRepo-persist");
    private final Timer loadTimer = TimingReports.newTimer("cassandra.mailRepo-load");
    private final Timer createdBeforeTimer = TimingReports.newTimer("cassandra.mailRepo-findCreatedBefore");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.mailRepo-delete");
    private final Timer deleteMailsOlderThenTimer = TimingReports.newTimer("cassandra.mailRepo-deleteOlderThen");
    private final Timer getCreationDateTimer = TimingReports.newTimer("cassandra.mailRepo-getCreationDate");
    private final Timer streamMailCreationDatesByDayTimer = TimingReports.newTimer("cassandra.mailRepo-streamMailCreationDatesByDay");

    private static final String FIELD_CREATION_DATE = "mail_creation_date";
    private static final String FIELD_MAIL_ID = "mail_id";

    public DefaultCassandraMailRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
    }

    @Override
    protected void doPersist(String messageId, byte[] compress) {
        try (Timer.Context ignored = persistTimer.time()) {
            Date creationDate = new Date();
            DateTime creationDateTime = new DateTime(creationDate);
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.INSERT_MAIL.bind(this, messageId, ByteBuffer.wrap(compress)));
            batch.add(Statements.INSERT_MAIL_CREATION_IDX.bind(this, messageId, creationDate));
            batch.add(Statements.INSERT_MAIL_CREATION_IDX_BY_DAY.bind(this, creationDateTime.getYear(), creationDateTime.getMonthOfYear(),
                    creationDateTime.getDayOfMonth(), creationDate, messageId));
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
    @Deprecated
    /**
     * Should not be used as it does the query with ALLOW FILTERING
     */
    public void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        try (Timer.Context ignored = deleteMailsOlderThenTimer.time()) {
            getMailIdsOlderThan(time, maxResults).forEach(this::deleteMail);
        }
    }

    protected Stream<String> getMailIdsOlderThan(DateTime time, int maxResults) {
        try (Timer.Context ignored = createdBeforeTimer.time()) {
            Statement bound = Statements.DEPRECATED_SELECT_MAIL_ID_WHERE_MAIL_CREATION_OLDER_THAN.bind(this, time.toDate(), maxResults);
            ResultSet resultset = session.execute(bound);
            return StreamUtils.toStream(resultset).map(row -> row.getString("mail_id"));
        }
    }

    @Override
    public void deleteMail(String messageId) {
        deleteMail(messageId, getMailCreationDate(messageId));
    }

    @Override
    public void deleteMail(String messageId, DateTime creationDate) {
        try (Timer.Context ignored = deleteTimer.time()) {
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.DELETE_BY_ID.bind(this, messageId));
            batch.add(Statements.DELETE_MAIL_CREATION_IDX_BY_ID.bind(this, messageId));
            if (creationDate != null) {
                batch.add(Statements.DELETE_MAIL_CREATION_IDX_BY_DAY.bind(this, creationDate.getYear(), creationDate.getMonthOfYear(),
                        creationDate.getDayOfMonth(), creationDate.toDate(), messageId));
            }
            session.execute(batch);
        }
    }

    @Override
    public DateTime getMailCreationDate(String mailId) {
        try (Timer.Context ignored = getCreationDateTimer.time()) {
            Statement bound = Statements.SELECT_MAIL_CREATION_DATE.bind(this, mailId);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            if (row == null) {
                return null;
            }
            return new DateTime(row.getDate(FIELD_CREATION_DATE));
        }
    }

    @Override
    public Stream<MailCreationDate> streamMailCreationDatesByDay(int year, int month, int day) {
        try (Timer.Context ignored = streamMailCreationDatesByDayTimer.time()) {
            Statement bound = Statements.SELECT_MAIL_CREATION_IDXS_BY_DAY.bind(this, year, month, day);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new MailCreationDate(row.getString(FIELD_MAIL_ID), row.getDate(FIELD_CREATION_DATE)));
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
        INSERT_MAIL_CREATION_IDX("INSERT INTO core_mail_creation_desc_index (mail_id, mail_creation_date) VALUES (?,?)", true),
        INSERT_MAIL_CREATION_IDX_BY_DAY("INSERT INTO core_mail_creation_desc_idx_by_day (year, month, day, mail_creation_date, mail_id) VALUES (?, ?, ?, ?, ?)", true),
        SELECT_MAIL_CREATION_DATE("SELECT mail_creation_date FROM core_mail_creation_desc_index WHERE mail_id = ?"),
        SELECT_MAIL_CREATION_IDXS_BY_DAY("SELECT mail_id, mail_creation_date FROM core_mail_creation_desc_idx_by_day WHERE year = ? AND month = ? AND day = ?"),
        DEPRECATED_SELECT_MAIL_ID_WHERE_MAIL_CREATION_OLDER_THAN("SELECT mail_id FROM core_mail_creation_desc_index WHERE mail_creation_date <= ? LIMIT ? ALLOW FILTERING"),
        DELETE_MAIL_CREATION_IDX_BY_ID("DELETE FROM core_mail_creation_desc_index WHERE mail_id=?", true),
        DELETE_MAIL_CREATION_IDX_BY_DAY("DELETE FROM core_mail_creation_desc_idx_by_day WHERE year = ? AND month = ? AND day = ? AND mail_creation_date = ? AND mail_id = ?", true);

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

        public Statement bind(DefaultCassandraMailRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(DefaultCassandraMailRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
