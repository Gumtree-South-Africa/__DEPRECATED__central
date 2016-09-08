package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.InvalidQueryException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class StatementsBase {
    String cql;

    boolean modifying;

    public StatementsBase(String cql, boolean modifying) {
        this.cql = cql;
        this.modifying = modifying;
    }

    public Statement bind(CassandraRepository repository, Object... values) {
        return repository.getPreparedStatements()
          .get(this)
          .bind(values)
          .setConsistencyLevel(getConsistencyLevel(repository))
          .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
    }

    public ConsistencyLevel getConsistencyLevel(CassandraRepository repository) {
        return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
    }

    public static Map<StatementsBase, PreparedStatement> prepare(Class<? extends StatementsBase> statementsClass, Session session) {
        List<StatementsBase> statements = new ArrayList<>();

        // Effectively treats the provided class like an enum of StatementsBase - avoids having to either keep an external list (suffers
        // from static-vs-constructor init order) or keeping an application-global registry of cached prepared statements

        for (Field field : statementsClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);

                try {
                    statements.add((StatementsBase) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(format("Unable to extract static StatementsBase from class %s", statementsClass));
                }
            }
        }

        return statements.stream().collect(Collectors.toMap(
          (statement) -> statement,
          (statement) -> {
              try {
                  return session.prepare(statement.cql);
              } catch (InvalidQueryException e) {
                  throw new IllegalArgumentException(format("Could not prepare statement '%s'", statement.cql), e);
              }
          })
        );
    }
}
