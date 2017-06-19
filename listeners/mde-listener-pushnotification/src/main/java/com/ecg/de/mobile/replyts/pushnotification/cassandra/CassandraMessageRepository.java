package com.ecg.de.mobile.replyts.pushnotification.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.ecg.de.mobile.replyts.pushnotification.JsonConverter;
import com.ecg.de.mobile.replyts.pushnotification.model.Message;
import com.ecg.de.mobile.replyts.pushnotification.model.MessageMetadata;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CassandraMessageRepository implements CassandraRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraMessageRepository.class);

    @Autowired
    @Qualifier("cassandraSessionForMb")
    private Session session;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;

    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    @Autowired
    private JsonConverter jsonConverter;

    private Map<StatementsBase, PreparedStatement> preparedStatements;

    @PostConstruct
    private void initializeStatements() {
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
    }

    public Optional<Message> getLastMessage(String userId, String conversationId) {
        Row rowMaybe = session.execute(Statements.SELECT_CONVERSATION_LAST_MESSAGE.bind(this, userId, conversationId)).one();
        return Optional.ofNullable(rowMaybe).map(row -> {
            UUID messageId = row.getUUID("msgid");
            MessageMetadata metadata = fromMessageMetadataJson(
                    userId,
                    conversationId,
                    messageId.toString(),
                    row.getString("metadata")
            );
            return new Message(row.getUUID("msgid"), row.getString("type"), metadata);
        });
    }

    private MessageMetadata fromMessageMetadataJson(String userId, String conversationId, String messageId, String jsonValue) {
        try {
            return jsonConverter.toObject(jsonValue, MessageMetadata.class);
        } catch (IOException e) {
            LOG.error("Could not deserialize message metadata of userId {}, conversationId {} and messageId {}, json: {}",
                    userId, conversationId, messageId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public ConsistencyLevel getReadConsistency() {
        return cassandraReadConsistency;
    }

    @Override
    public ConsistencyLevel getWriteConsistency() {
        return cassandraWriteConsistency;
    }

    @Override
    public Map<StatementsBase, PreparedStatement> getPreparedStatements() {
        return preparedStatements;
    }

    static class Statements extends StatementsBase {
        static Statements SELECT_CONVERSATION_LAST_MESSAGE = new Statements(
                "SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? ORDER BY msgid DESC LIMIT 1",
                false);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }
}