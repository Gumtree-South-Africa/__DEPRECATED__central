package com.ecg.replyts.core.runtime.persistence.mail;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;

import java.nio.ByteBuffer;
import java.util.Map;

public class CassandraHeldMailRepository implements HeldMailRepository, CassandraRepository {
    private static final String FIELD_MAIL_DATA = "mail_data";

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer readTimer = TimingReports.newTimer("cassandra.heldMailRepo-read");
    private final Timer writeTimer = TimingReports.newTimer("cassandra.heldMailRepo-write");
    private final Timer removeTimer = TimingReports.newTimer("cassandra.heldMailRepo-remove");

    private Map<StatementsBase, PreparedStatement> preparedStatements;

    public CassandraHeldMailRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
    }

    @Override
    public byte[] read(String messageId) {
        try (Timer.Context ignored = readTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_INBOUND_MAIL.bind(this, messageId));

            Row row = result.one();

            if (row == null) {
                throw new RuntimeException("Could not load held mail data by message id #" + messageId);
            }

            return row.getBytes(FIELD_MAIL_DATA).array();
        }
    }

    @Override
    public void write(String messageId, byte[] content) {
        try (Timer.Context ignored = writeTimer.time()) {
            session.execute(Statements.UPDATE_INBOUND_MAIL.bind(this, ByteBuffer.wrap(content), messageId));
        }
    }

    @Override
    public void remove(String messageId) {
        try (Timer.Context ignored = removeTimer.time()) {
            session.execute(Statements.DELETE_INBOUND_MAIL.bind(this, messageId));
        }
    }

    @Override
    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    @Override
    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    @Override
    public Map<StatementsBase, PreparedStatement> getPreparedStatements() {
        return preparedStatements;
    }

    static class Statements extends StatementsBase {
        static Statements SELECT_INBOUND_MAIL = new Statements("SELECT message_id, mail_data FROM core_held_mail WHERE message_id = ?", false);
        static Statements UPDATE_INBOUND_MAIL = new Statements("UPDATE core_held_mail SET mail_data = ? WHERE message_id = ?", true);
        static Statements DELETE_INBOUND_MAIL = new Statements("DELETE FROM core_held_mail WHERE message_id = ?", true);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }
}
