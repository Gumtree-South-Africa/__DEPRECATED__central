package com.ecg.comaas.r2cmigration.difftool.repo;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;

@Repository
public class CassConversationBlockRepo {

    private static final Logger LOG = LoggerFactory.getLogger(CassConversationBlockRepo.class);
    private static final Timer GET_BY_ID_CASS_TIMER = TimingReports.newTimer("cass-getById");
    private static final String SELECT_FROM_CONVERSATION_BLOCK = "SELECT * FROM mb_conversationblock WHERE conversation_id = ?";
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_JSON_VALUE = "json_value";

    private final PreparedStatement getByConvID;

    private ObjectMapper objectMapper;
    private Session session;
    private ConsistencyLevel cassandraReadConsistency;

    @Autowired
    public CassConversationBlockRepo(
            @Qualifier("cassandraSessionForCore") Session session,
            JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer,
            @Value("${persistence.cassandra.consistency.read:#{null}}") ConsistencyLevel cassandraReadConsistency
    ) {
        this.session = session;
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
        this.cassandraReadConsistency = cassandraReadConsistency;

        this.getByConvID = session.prepare(SELECT_FROM_CONVERSATION_BLOCK);
    }

    public ConversationBlock getById(String conversationId) {
        try (Timer.Context ignored = GET_BY_ID_CASS_TIMER.time()) {
            Statement statement = bind(getByConvID, conversationId);
            ResultSet resultset = session.execute(statement);
            Row row = resultset.one();

            if (row == null) {
                LOG.warn("Conversation block with id {} was not found in Cassandra", conversationId);
                return null;
            } else {
                return rowToConversationBlock(row);
            }
        }
    }

    public Statement bind(PreparedStatement statement, Object... values) {
        BoundStatement bs = statement.bind(values);
        return bs.setConsistencyLevel(cassandraReadConsistency);
    }

    private ConversationBlock rowToConversationBlock(Row row) {
        String jsonValue = row.getString(FIELD_JSON_VALUE);
        try {
            return objectMapper.readValue(jsonValue, ConversationBlock.class);
        } catch (IOException e) {
            String conversationId = row.getString(FIELD_CONVERSATION_ID);
            throw new RuntimeException("Couldn't parse json_value for conversationBlock with id " + conversationId, e);
        }
    }
}
