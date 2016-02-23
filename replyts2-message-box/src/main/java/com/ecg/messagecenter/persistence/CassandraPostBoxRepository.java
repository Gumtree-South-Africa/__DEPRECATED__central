package com.ecg.messagecenter.persistence;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Persists {@link PostBox}es in Cassandra.
 * <p>
 * Two tables are used: {@code postbox} and {@code postbox_modification_idx}. The latter is used to find which
 * post boxes can be cleaned.
 * </p>
 * <p>
 * Table {@code postbox} stores a post box in multiple rows, one row per conversation. Each row contains the unread messages
 * count and a preview of the last message. In the case of contention (e.g. one client decreases unread count, another
 * adds a message), conversation representation can be 'outdated'. However, the next time a message is sent the
 * representation will fix itself.
 * </p>
 * <p>
 * Lets look at the contention in more detail.
 * </p>
 * <p>
 * There are two possible operations on a conversation: <ul>
 * <li>Reset unread message count to 0.</li>
 * <li>Update preview of the latest message and increase unread message count with 1.</li>
 * </ul>
 * <p/>
 * (We can also delete conversations, but that only happens when it is old. The risk of contention then is negligible.)
 * </p>
 * <p>
 * In the case of contention either operation can 'win'.
 * </p>
 * <p>
 * When the first wins, the preview contains an older message. However, a push notification and/or email with the new
 * message has been sent regardless so the user will see it. The user will also see it when he/she opens the
 * conversation (that data does not come from the post box). Only in the conversation overview screen will there be
 * an older message. The next message will correct the view.
 * </p>
 * <p>
 * When the second operation wins, the unread counter is not reset. That is fine because there is a new message (we
 * just report the wrong number of unread messages). When the user reads the newly added message, the client will reset
 * the unread count again.<br/>
 * Note: you could choose to report the number of unread conversation instead. In that case we would never report
 * the wrong number.
 * </p>
 */
public class CassandraPostBoxRepository implements PostBoxRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPostBoxRepository.class);

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private ObjectMapper objectMapper;

    private final Timer writeTimer = TimingReports.newTimer("cassandra.postBoxRepo.write");
    private final Timer findByIdTimer = TimingReports.newTimer("cassandra.postBoxRepo.findById");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.postBoxRepo.delete");
    private final Timer findByModificationBeforeTimer = TimingReports.newTimer("cassandraPostBoxRepo.findByModificationBefore");
    private final Timer getConversationThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo.getConversationThread");
    private final Timer addReplaceConversationThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo.addReplaceConversationThread");

    public CassandraPostBoxRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
    }

    @Override
    public void write(PostBox postBox) {
        try (Timer.Context ignored = writeTimer.time()) {
            String postboxId = postBox.getUserId();
            BatchStatement batch = new BatchStatement();

            // Remove any deleted conversations from this postbox
            postBox
                    .flushRemovedThreads()
                    .forEach(conversationId ->
                            batch.add(Statements.DELETE_BY_CONVERSATION_ID.bind(this, postboxId, conversationId))
                    );

            // Insert/Update the existing conversation threads
            for (ConversationThread ct : postBox.getConversationThreads()) {
                if (ct.isModified()) {
                    try {
                        String jsonValue = this.objectMapper.writeValueAsString(ct);
                        batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue, postboxId, ct.getConversationId()));

                    } catch (JsonProcessingException e) {
                        LOGGER.error(
                                "Could not marshall conversation thread for postbox {} conversation {}",
                                postboxId, ct.getConversationId(), e);
                    }
                }
            }

            // Update the modified index
            Statement bound = Statements.SELECT_MODIFICATION_IDX.bind(this, postboxId);
            ResultSet resultset = session.execute(bound);
            resultset.forEach(row -> batch.add(Statements.DELETE_MODIFICATION_IDX_ONE.bind(this, postboxId, row.getDate("modification_date"))));
            batch.add(Statements.INSERT_MODIFICATION_IDX.bind(this, postboxId, postBox.getLastModification().toDate()));

            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    @Override
    public PostBox byId(final String id) {
        try (Timer.Context ignored = findByIdTimer.time()) {
            final List<ConversationThread> conversationThreads = new ArrayList<>();
            ResultSet result = session.execute(Statements.SELECT_BY_POSTBOX_ID.bind(this, id));
            result.forEach(row -> {
                        Optional<ConversationThread> ctOptional = toConversationThread(id, row.getString("conversation_id"), row.getString("json_value"));
                        if (ctOptional.isPresent()) {
                            conversationThreads.add(ctOptional.get());
                        }
                    }
            );
            return new PostBox(id, conversationThreads);
        }
    }

    @Override
    public void cleanupLongTimeUntouchedPostBoxes(DateTime time) {
        ResultSet resultset;
        try (Timer.Context ignored = findByModificationBeforeTimer.time()) {
            Statement bound = Statements.SELECT_WHERE_MODIFICATION_BEFORE.bind(this, time.toDate());
            resultset = session.execute(bound);
        }
        resultset.forEach(row -> deletePostbox(row.getString("postbox_id")));
    }

    @Override
    public Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationThreadTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_THREAD.bind(this, postBoxId, conversationId));
            Row row = resultSet.one();
            if (row != null) {
                return toConversationThread(postBoxId, row.getString("conversation_id"), row.getString("json_value"));
            } else {
                return Optional.absent();
            }
        }
    }

    @Override
    public void addReplaceConversationThread(String postBoxId, ConversationThread conversationThread) {
        try (Timer.Context ignored = addReplaceConversationThreadTimer.time()) {
            BatchStatement batch = new BatchStatement();

            try {
                String jsonValue = this.objectMapper.writeValueAsString(conversationThread);
                batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue, postBoxId, conversationThread.getConversationId()));
            } catch (JsonProcessingException e) {
                LOGGER.error(
                        "Could not marshall conversation thread for postbox id {} and conversation id {}",
                        postBoxId, conversationThread.getConversationId(), e);
            }

            // update the postBox modified index
            Statement bound = Statements.SELECT_MODIFICATION_IDX.bind(this, postBoxId);
            ResultSet resultset = session.execute(bound);
            resultset.forEach(row -> batch.add(Statements.DELETE_MODIFICATION_IDX_ONE.bind(this, postBoxId, row.getDate("modification_date"))));
            batch.add(Statements.INSERT_MODIFICATION_IDX.bind(this, postBoxId, conversationThread.getModifiedAt().toDate()));

            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private Optional<ConversationThread> toConversationThread(String postboxId, String conversationId, String jsonValue) {
        try {
            return Optional.of(objectMapper.readValue(jsonValue, ConversationThread.class));
        } catch (IOException e) {
            LOGGER.error("Could not unmarshal post box {} conversation {} json: {}", postboxId, conversationId, jsonValue, e);
            return Optional.absent();
        }
    }

    private void deletePostbox(String postboxId) {
        try (Timer.Context ignored = deleteTimer.time()) {
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.DELETE_BY_POSTBOX_ID.bind(this, postboxId));
            batch.add(Statements.DELETE_MODIFICATION_IDX.bind(this, postboxId));
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    enum Statements {

        SELECT_BY_POSTBOX_ID("SELECT * FROM mb_postbox WHERE postbox_id=?"),
        DELETE_BY_POSTBOX_ID("DELETE FROM mb_postbox WHERE postbox_id=?", true),
        DELETE_BY_CONVERSATION_ID("DELETE FROM mb_postbox WHERE postbox_id=? AND conversation_id=?", true),
        SELECT_CONVERSATION_THREAD("SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id=? AND conversation_id=?"),
        UPDATE_CONVERSATION_THREAD("UPDATE mb_postbox SET json_value=? WHERE postbox_id=? AND conversation_id=?", true),
        SELECT_MODIFICATION_IDX("SELECT postbox_id, modification_date FROM mb_postbox_modification_idx WHERE postbox_id=?"),
        INSERT_MODIFICATION_IDX("INSERT INTO mb_postbox_modification_idx (postbox_id, modification_date) VALUES (?,?)", true),
        DELETE_MODIFICATION_IDX("DELETE FROM mb_postbox_modification_idx WHERE postbox_id=?", true),
        SELECT_WHERE_MODIFICATION_BEFORE("SELECT postbox_id FROM mb_postbox_modification_idx WHERE modification_date <= ? ALLOW FILTERING"),
        DELETE_MODIFICATION_IDX_ONE("DELETE FROM mb_postbox_modification_idx WHERE postbox_id=? AND modification_date=?", true);

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

        public Statement bind(CassandraPostBoxRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraPostBoxRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}