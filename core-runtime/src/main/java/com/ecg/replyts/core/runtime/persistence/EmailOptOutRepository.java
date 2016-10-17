package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import java.util.Map;

public class EmailOptOutRepository implements CassandraRepository {
    private final Session session;
    private final Map<StatementsBase, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    public EmailOptOutRepository(
            Session session,
            ConsistencyLevel readConsistency,
            ConsistencyLevel writeConsistency
    ) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
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

    public void turnOnEmail(String userId) {
        session.execute(Statements.EMAIL_OPT_IN.bind(this, userId));
    }

    public void turnOffEmail(String userId) {
        session.execute(Statements.EMAIL_OPT_OUT.bind(this, userId));
    }

    public boolean isEmailTurnedOn(String userId) {
        return session.execute(Statements.IS_OPT_OUT.bind(this, userId)).one().getLong("count") == 0;
    }

    static class Statements extends StatementsBase {
        static Statements EMAIL_OPT_OUT = new Statements("INSERT INTO core_email_opt_out (user_id) VALUES (?) IF NOT EXISTS", true);
        static Statements EMAIL_OPT_IN = new Statements("DELETE FROM core_email_opt_out WHERE user_id = ?", true);
        static Statements IS_OPT_OUT = new Statements("SELECT COUNT(*) FROM core_email_opt_out WHERE user_id = ?", false);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }

}
