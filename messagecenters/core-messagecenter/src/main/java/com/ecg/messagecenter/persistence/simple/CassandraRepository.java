package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;

import java.util.Map;

public interface CassandraRepository {
    ConsistencyLevel getReadConsistency();

    ConsistencyLevel getWriteConsistency();

    Map<StatementsBase, PreparedStatement> getPreparedStatements();
}
