package com.ecg.messagebox.persistence;

import com.datastax.driver.core.*;
import com.google.common.collect.ImmutableMap;

import java.util.EnumMap;
import java.util.Map;

class CassandraTemplate {

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;
    private final Map<Statements, PreparedStatement> preparedStatements;

    CassandraTemplate(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        Map<Statements, PreparedStatement> statements = new EnumMap<>(Statements.class);
        for (Statements statement : Statements.values()) {
            statements.put(statement, session.prepare(statement.getCql()));
        }
        this.preparedStatements = ImmutableMap.copyOf(statements);
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
    }

    ResultSetFuture executeAsync(Statement statement) {
        return session.executeAsync(statement);
    }

    ResultSet execute(Statements statement, Object... values) {
        return session.execute(boundStatement(statement, values));
    }

    ResultSet execute(Statement statement) {
        return session.execute(statement);
    }

    Statement bind(Statements statement, Object... values) {
        return boundStatement(statement, values);
    }

    private Statement boundStatement(Statements statement, Object[] values) {
        return preparedStatements
                .get(statement)
                .bind(values)
                .setConsistencyLevel(getConsistencyLevel(statement.isModifying()))
                .setIdempotent(!statement.isModifying());
    }

    private ConsistencyLevel getConsistencyLevel(boolean modifying) {
        return modifying ? writeConsistency : readConsistency;
    }
}
