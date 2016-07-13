package com.ecg.messagecenter.persistence.cassandra;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagecenter.persistence.*;
import com.ecg.messagecenter.util.StreamUtils;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

/**
 * Persists {@link ConversationThread}s and unread counters in Cassandra.
 * <p>
 * Two tables are used: {@code mb_postbox} and {@code mb_unread_counters}.
 * </p>
 * <p>
 * Table {@code mb_postbox} stores a post box in multiple rows, one row per conversation. Each row contains the unread messages
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
 * <p>
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
 * <p>
 * TODO: rewrite the above now that we store counters separately
 * </p>
 */
public class DefaultCassandraPostBoxRepository implements CassandraPostBoxRepository {

    private final int ttlResponseData;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCassandraPostBoxRepository.class);

    private static final String FIELD_USER_ID = "userid";
    private static final String FIELD_CONVERSATION_ID = "convid";
    private static final String FIELD_CONVERSATION_TYPE = "convtype";
    private static final String FIELD_CREATION_DATE = "createdate";
    private static final String FIELD_RESPONSE_SPEED = "responsespeed";

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private ObjectMapper objectMapper;

    private final Timer getPostBoxTimer = TimingReports.newTimer("cassandra.postBoxRepo.findById");
    private final Timer getUnreadCountsTimer = TimingReports.newTimer("cassandra.postBoxRepo.getUnreadCounts");
    private final Timer selectConversationThreadIdsTimer = TimingReports.newTimer("cassandra.postBoxRepo.selectConversationThreadIds");
    private final Timer getConversationThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo.getConversationThread");
    private final Timer addReplaceConversationThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo.addReplaceConversationThread");
    private final Timer selectConversationUnreadMessagesCountTimer = TimingReports.newTimer("cassandra.postBoxRepo.selectConversationUnreadMessagesCount");
    private final Timer updateConversationUnreadMessagesCountTimer = TimingReports.newTimer("cassandra.postBoxRepo.updateConversationUnreadMessagesCount");
    private final Timer selectResponseDataTimer = TimingReports.newTimer("cassandra.postBoxRepo.selectResponseData");
    private final Timer updateResponseDataTimer = TimingReports.newTimer("cassandra.postBoxRepo.addOrUpdateResponseDataAsync");

    @Autowired
    public DefaultCassandraPostBoxRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency,
                                             @Value("${persistence.cassandra.ttl.response.data:31536000}") int ttlResponseData) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
        this.ttlResponseData = ttlResponseData;
    }

    @Override
    public PostBox getPostBox(String postBoxId) {
        try (Timer.Context ignored = getPostBoxTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_POSTBOX_CONVERSATION_UNREAD_COUNTS.bind(this, postBoxId));
            Map<String, Integer> conversationUnreadCounts = StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(
                            row -> row.getString("conversation_id"),
                            row -> row.getInt("num_unread")));

            List<ConversationThread> conversationThreads = new ArrayList<>();
            ResultSet result = session.execute(Statements.SELECT_POSTBOX.bind(this, postBoxId));
            result.forEach(row -> {
                String conversationId = row.getString("conversation_id");
                String json_value = row.getString("json_value");
                int unreadCount = conversationUnreadCounts.getOrDefault(conversationId, 0);

                Optional<ConversationThread> ctOptional = toConversationThread(
                        postBoxId,
                        conversationId,
                        json_value,
                        unreadCount);

                ctOptional.map(conversationThreads::add);
            });

            return new PostBox(postBoxId, conversationThreads);
        }
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_POSTBOX_UNREAD_COUNTS.bind(this, postBoxId));
            CountersConsumer counters = StreamUtils.toStream(result)
                    .collect(CountersConsumer::new, CountersConsumer::add, CountersConsumer::reduce);
            return new PostBoxUnreadCounts(counters.numUnreadConversations, counters.numUnreadMessages);
        }
    }

    private static class CountersConsumer {

        private int numUnreadConversations = 0;
        private int numUnreadMessages = 0;

        public void add(Row row) {
            int numUnreadMessagesInConversation = row.getInt("num_unread");
            numUnreadConversations += numUnreadMessagesInConversation > 0 ? 1 : 0;
            numUnreadMessages += numUnreadMessagesInConversation;
        }

        public void reduce(CountersConsumer countersConsumer) {
            numUnreadConversations += countersConsumer.numUnreadConversations;
            numUnreadMessages += countersConsumer.numUnreadMessages;
        }
    }

    @Override
    public Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationThreadTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_THREAD.bind(this, postBoxId, conversationId));
            Row row = resultSet.one();
            if (row != null) {
                int numUnreadMessages = getConversationUnreadMessagesCount(postBoxId, conversationId);
                return toConversationThread(postBoxId, row.getString("conversation_id"), row.getString("json_value"), numUnreadMessages);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public int getConversationUnreadMessagesCount(String postBoxId, String conversationId) {
        try (Timer.Context ignored = selectConversationUnreadMessagesCountTimer.time()) {
            Row unreadMessagesResult = session.execute(Statements.SELECT_CONVERSATION_UNREAD_COUNT.bind(this, postBoxId, conversationId)).one();
            return unreadMessagesResult == null ? 0 : unreadMessagesResult.getInt("num_unread");
        }
    }

    @Override
    public void incrementConversationUnreadMessagesCount(String postBoxId, String conversationId) {
        int numUnreadMessages = getConversationUnreadMessagesCount(postBoxId, conversationId);
        try (Timer.Context ignored = updateConversationUnreadMessagesCountTimer.time()) {
            Statement bound = Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, numUnreadMessages + 1, postBoxId, conversationId);
            session.execute(bound);
        }
    }

    @Override
    public void resetConversationUnreadMessagesCountAsync(String postBoxId, String conversationId) {
        Statement bound = Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, postBoxId, conversationId);
        session.executeAsync(bound);
    }

    @Override
    public void addReplaceConversationThread(String postBoxId, ConversationThread conversationThread) {
        try (Timer.Context ignored = addReplaceConversationThreadTimer.time()) {
            String jsonValue;
            try {
                jsonValue = objectMapper.writeValueAsString(conversationThread);
            } catch (JsonProcessingException e) {
                LOGGER.error(
                        "Could not serialize conversation thread for postbox id {} and conversation id {}",
                        postBoxId, conversationThread.getConversationId(), e);
                throw new RuntimeException(e);
            }

            Statement statement = Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue, postBoxId, conversationThread.getConversationId());
            session.execute(statement);
        }
    }

    @Override
    public void deleteConversationThreadsAsync(String postBoxId, List<String> conversationIds) {
        conversationIds.stream().forEach(conversationId -> {
            BatchStatement batch = new BatchStatement();
            batch.add(Statements.DELETE_CONVERSATION_THREAD.bind(this, postBoxId, conversationId));
            batch.add(Statements.DELETE_CONVERSATION_UNREAD_COUNT.bind(this, postBoxId, conversationId));
            batch.setConsistencyLevel(getWriteConsistency());
            session.executeAsync(batch);
        });
    }

    @Override
    public List<String> getConversationThreadIds(String postBoxId) {
        try (Timer.Context ignored = selectConversationThreadIdsTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_CONVERSATION_THREAD_IDS.bind(this, postBoxId));
            return StreamUtils.toStream(result)
                    .map(row -> row.getString("conversation_id"))
                    .collect(Collectors.toList());
        }
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

    private ResponseData rowToResponseData(Row row) {
        return new ResponseData(row.getString(FIELD_USER_ID), row.getString(FIELD_CONVERSATION_ID),
                new DateTime(row.getDate(FIELD_CREATION_DATE)), MessageType.get(row.getString(FIELD_CONVERSATION_TYPE)), row.getInt(FIELD_RESPONSE_SPEED));
    }

    private Optional<ConversationThread> toConversationThread(String postboxId, String conversationId, String jsonValue, int numUnreadMessages) {
        try {
            ConversationThread conversationThread = objectMapper.readValue(jsonValue, ConversationThread.class);
            conversationThread.setNumUnreadMessages(numUnreadMessages);
            return Optional.of(conversationThread);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize post box {} conversation {} json: {}", postboxId, conversationId, jsonValue, e);
            return Optional.empty();
        }
    }

    enum Statements {

        SELECT_POSTBOX("SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id=?"),
        DELETE_CONVERSATION_THREAD("DELETE FROM mb_postbox WHERE postbox_id=? AND conversation_id=?", true),
        SELECT_CONVERSATION_THREAD("SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id=? AND conversation_id=?"),
        UPDATE_CONVERSATION_THREAD("UPDATE mb_postbox SET json_value=? WHERE postbox_id=? AND conversation_id=?", true),
        SELECT_CONVERSATION_THREAD_IDS("SELECT conversation_id FROM mb_postbox WHERE postbox_id=?"),
        UPDATE_CONVERSATION_UNREAD_COUNT("UPDATE mb_unread_counters SET num_unread=? WHERE postbox_id=? and conversation_id=?", true),
        SELECT_CONVERSATION_UNREAD_COUNT("SELECT num_unread FROM mb_unread_counters WHERE postbox_id=? and conversation_id=?"),
        SELECT_POSTBOX_UNREAD_COUNTS("SELECT num_unread FROM mb_unread_counters WHERE postbox_id=?"),
        SELECT_POSTBOX_CONVERSATION_UNREAD_COUNTS("SELECT conversation_id, num_unread FROM mb_unread_counters WHERE postbox_id=?"),
        // TODO: use following two statements in postbox/conversation thread cleanup job (which is not yet written)
        DELETE_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_unread_counters WHERE postbox_id=? and conversation_id=?", true),
        DELETE_POSTBOX_UNREAD_COUNTS("DELETE FROM mb_unread_counters WHERE postbox_id=?", true),

        // response rate and speed
        SELECT_RESPONSE_DATA("SELECT userid, convid, convtype, createdate, responsespeed FROM mb_response_data WHERE userid=?"),
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

        public Statement bind(DefaultCassandraPostBoxRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository));
        }

        private ConsistencyLevel getConsistencyLevel(DefaultCassandraPostBoxRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
