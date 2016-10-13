package com.ecg.messagebox.persistence;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagebox.persistence.jsonconverter.JsonConverter;
import com.ecg.messagecenter.persistence.ResponseData;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

@Component
public class DefaultResponseDataRepository implements ResponseDataRepository {

    private final Timer selectResponseDataTimer = newTimer("cassandra.postBoxRepo.v2.selectResponseData");
    private final Timer updateResponseDataTimer = newTimer("cassandra.postBoxRepo.addOrUpdateResponseDataAsync");

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private Map<Statements, PreparedStatement> preparedStatements;

    private final int ttlResponseData;

    @Autowired
    public DefaultResponseDataRepository(
            @Qualifier("cassandraSession") Session session,
            @Qualifier("cassandraReadConsistency") ConsistencyLevel readConsistency,
            @Qualifier("cassandraWriteConsistency") ConsistencyLevel writeConsistency,
            @Value("${persistence.cassandra.ttl.response.data:31536000}") int ttlResponseData
    ) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
        this.ttlResponseData = ttlResponseData;
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = selectResponseDataTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_RESPONSE_DATA.bind(this, userId));
            return toStream(result)
                    .map(this::rowToResponseData)
                    .collect(Collectors.toList());
        }
    }

    private ResponseData rowToResponseData(Row row) {
        return new ResponseData(row.getString("userid"), row.getString("convid"),
                new DateTime(row.getDate("createdate")), com.ecg.messagecenter.persistence.MessageType.get(row.getString("convtype")), row.getInt("responsespeed"));
    }

    @Override
    public void addOrUpdateResponseDataAsync(ResponseData responseData) {
        try (Timer.Context ignored = updateResponseDataTimer.time()) {
            int secondsSinceConvCreation = Seconds.secondsBetween(responseData.getConversationCreationDate(), DateTime.now()).getSeconds();

            Statement bound = Statements.UPDATE_RESPONSE_DATA.bind(this,
                    ttlResponseData - secondsSinceConvCreation,
                    responseData.getConversationType().name().toLowerCase(),
                    responseData.getConversationCreationDate().toDate(),
                    responseData.getResponseSpeed(),
                    responseData.getUserId(),
                    responseData.getConversationId());
            session.executeAsync(bound);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    enum Statements {
        SELECT_RESPONSE_DATA("SELECT userid, convid, convtype, createdate, responsespeed FROM mb_response_data WHERE userid=? LIMIT 100"),
        UPDATE_RESPONSE_DATA("UPDATE mb_response_data USING TTL ? SET convtype=?, createdate=?, responsespeed=? WHERE userid=? AND convid=?");

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

        public Statement bind(DefaultResponseDataRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository));
        }

        private ConsistencyLevel getConsistencyLevel(DefaultResponseDataRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
