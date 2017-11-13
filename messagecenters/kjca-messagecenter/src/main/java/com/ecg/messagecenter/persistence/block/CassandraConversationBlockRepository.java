package com.ecg.messagecenter.persistence.block;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

public class CassandraConversationBlockRepository implements ConversationBlockRepository, CassandraRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraConversationBlockRepository.class);

    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_DATE = "modification_date";
    private static final String FIELD_JSON_VALUE = "json_value";

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer byIdTimer = TimingReports.newTimer("cassandra.blockRepo-byId");
    private final Timer writeTimer = TimingReports.newTimer("cassandra.blockRepo-write");
    private final Timer cleanupTimer = TimingReports.newTimer("cassandra.blockRepo-cleanup");

    private Map<StatementsBase, PreparedStatement> preparedStatements;

    private ObjectMapper objectMapper;

    @PostConstruct
    public void initializePreparedStatements() {
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
    }

    public CassandraConversationBlockRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
    }

    @Override
    public ConversationBlock byId(String conversationId) {
        try (Timer.Context ignored = byIdTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_CONVERSATION_BLOCK.bind(this, conversationId));

            Row row = result.one();

            if (row == null) {
                throw new RuntimeException("Could not load conversation block object by conversation id #" + conversationId);
            }

            try {
                return objectMapper.readValue(row.getString(FIELD_JSON_VALUE), ConversationBlock.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not read convertible JSON value from table", e);
            }
        }
    }

    @Override
    public void write(ConversationBlock conversationBlock) {
        DateTime modificationDate = DateTime.now();

        try (Timer.Context ignored = writeTimer.time()) {
            String jsonValue = objectMapper.writeValueAsString(conversationBlock);

            session.execute(Statements.UPDATE_CONVERSATION_BLOCK.bind(this, jsonValue, conversationBlock.getConversationId()));
            session.execute(Statements.INSERT_CONVERSATION_BLOCK_IDX.bind(this, conversationBlock.getConversationId(), modificationDate.toDate()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize conversation block to JSON", e);
        }
    }

    @Override
    public void cleanup(DateTime before) {
        Date beforeDate = before.toDate();

        LOG.info("Cleanup: Deleting conversation blocks before {}", before);

        try (Timer.Context ignored = cleanupTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_CONVERSATION_BLOCK_BEFORE_DATE.bind(this, beforeDate));

            toStream(result).map(row -> row.getString(FIELD_CONVERSATION_ID)).forEach(conversationId -> {
                // Compare this modification to the latest modification for this conversation block

                ResultSet latestResult = session.execute(Statements.SELECT_CONVERSATION_BLOCK_LATEST.bind(this, conversationId));
                Date latest = toStream(latestResult).map(row -> row.getDate(FIELD_MODIFICATION_DATE)).findFirst().get();

                if (latest != null && latest.after(beforeDate)) {
                    // Only delete this _idx entry

                    session.execute(Statements.DELETE_CONVERSATION_BLOCK_IDX.bind(this, latest, conversationId));
                } else {
                    // Delete this conversation block and all its _idx entries

                    session.execute(Statements.DELETE_CONVERSATION_BLOCK.bind(this, conversationId));
                    session.execute(Statements.DELETE_CONVERSATION_BLOCK_IDX_ALL.bind(this, conversationId));
                }
            });
        }

        LOG.info("Cleanup: Finished deleting conversation blocks.");
    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
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
        static Statements SELECT_CONVERSATION_BLOCK = new Statements("SELECT conversation_id, json_value FROM mb_conversationblock WHERE conversation_id = ?", false);
        static Statements SELECT_CONVERSATION_BLOCK_BEFORE_DATE = new Statements("SELECT conversation_id FROM mb_conversationblock_idx WHERE modification_date < ? ALLOW FILTERING", false);
        static Statements SELECT_CONVERSATION_BLOCK_LATEST = new Statements("SELECT modification_date FROM mb_conversationblock_idx WHERE conversation_id = ? LIMIT 1", false);
        static Statements UPDATE_CONVERSATION_BLOCK = new Statements("UPDATE mb_conversationblock SET json_value = ? WHERE conversation_id = ?", true);
        static Statements INSERT_CONVERSATION_BLOCK_IDX = new Statements("INSERT INTO mb_conversationblock_idx(conversation_id, modification_date) VALUES(?, ?)", true);
        static Statements DELETE_CONVERSATION_BLOCK = new Statements("DELETE FROM mb_conversationblock WHERE conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_BLOCK_IDX = new Statements("DELETE FROM mb_conversationblock_idx WHERE modification_date = ? AND conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_BLOCK_IDX_ALL = new Statements("DELETE FROM mb_conversationblock_idx WHERE conversation_id = ?", true);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }
}
