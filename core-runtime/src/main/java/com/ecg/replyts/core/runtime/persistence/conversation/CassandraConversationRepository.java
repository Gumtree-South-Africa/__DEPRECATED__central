package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

/**
 * Cassandra backed conversation repository.
 */
public class CassandraConversationRepository implements MutableConversationRepository {
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_EVENT_ID = "event_id";
    private static final String FIELD_EVENT_JSON = "event_json";

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer getByIdTimer = TimingReports.newTimer("cassandra.conversationRepo-getById");
    private final Timer getBySecretTimer = TimingReports.newTimer("cassandra.conversationRepo-getBySecret");
    private final Timer isSecretAvailableTimer = TimingReports.newTimer("cassandra.conversationRepo-isSecretAvailable");
    private final Timer commitTimer = TimingReports.newTimer("cassandra.conversationRepo-commit");
    private final Timer modifiedBetweenTimer = TimingReports.newTimer("cassandra.conversationRepo-modifiedBetween");
    private final Timer streamingTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationIds");
    private final Timer modifiedBeforeTimer = TimingReports.newTimer("cassandra.conversationRepo-modifiedBefore");
    private final Timer findByIndexKeyTimer = TimingReports.newTimer("cassandra.conversationRepo-findByIndexKey");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.conversationRepo-delete");

    private ObjectMapper objectMapper;

    public CassandraConversationRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
    }

    @Override
    public MutableConversation getById(String conversationId) {
        try (Timer.Context ignored = getByIdTimer.time()) {
            return replayEvents(getConversationEvents(conversationId));
        }
    }

    private List<ConversationEvent> getConversationEvents(String conversationId) {
        Statement statement = Statements.SELECT_FROM_CONVERSATION_EVENTS.bind(this, conversationId);
        ResultSet resultset = session.execute(statement);
        return toStream(resultset)
                .map(this::rowToConversationEvent)
                .collect(Collectors.toList());
    }

    private ConversationEvent rowToConversationEvent(Row row) {
        String eventJsonWithClass = row.getString(FIELD_EVENT_JSON);
        String className = eventJsonWithClass.substring(0, eventJsonWithClass.indexOf("@@"));
        String eventJson = eventJsonWithClass.substring(eventJsonWithClass.indexOf("@@") + 2);
        try {
            return objectMapper.readValue(eventJson, (Class<? extends ConversationEvent>) Class.forName(className));
        } catch (Exception e) {
            String conversationId = row.getString(FIELD_CONVERSATION_ID);
            UUID eventId = row.getUUID(FIELD_EVENT_ID);
            throw new RuntimeException("Couldn't parse conversation event " + eventId + " in conversation " + conversationId, e);
        }
    }

    private MutableConversation replayEvents(List<ConversationEvent> events) {
        if (events.isEmpty()) {
            return null;
        }
        return new DefaultMutableConversation(ImmutableConversation.replay(events));
    }

    @Override
    public MutableConversation getBySecret(String secret) {
        try (Timer.Context ignored = getBySecretTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_ID_FROM_SECRET.bind(this, secret);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            if (row == null) {
                return null;
            }
            return getById(row.getString(FIELD_CONVERSATION_ID));
        }
    }

    @Override
    public boolean isSecretAvailable(String secret) {
        try (Timer.Context ignored = isSecretAvailableTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_ID_FROM_SECRET.bind(this, secret);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            return (row == null);
        }
    }

    @Override
    public List<String> listConversationsModifiedBetween(DateTime start, DateTime end) {
        try (Timer.Context ignored = modifiedBetweenTimer.time()) {
            Statement bound = Statements.SELECT_WHERE_MODIFICATION_BETWEEN.bind(this, start.toDate(), end.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID)).collect(Collectors.toList());
        }
    }

    @Override
    public Stream<String> streamConversationsModifiedBetween(DateTime start, DateTime end) {
        try (Timer.Context ignored = streamingTimer.time()) {
            Statement bound = Statements.SELECT_WHERE_MODIFICATION_BETWEEN.bind(this, start.toDate(), end.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID));
        }
    }

    @Override
    public List<String> listConversationsCreatedBetween(DateTime start, DateTime end) {
        throw new UnsupportedOperationException("listConversationsCreatedBetween not available for Cassandra");
    }

    @Override
    public List<String> listConversationsModifiedBefore(DateTime before, int maxResults) {
        try (Timer.Context ignored = modifiedBeforeTimer.time()) {
            Statement bound = Statements.SELECT_WHERE_MODIFICATION_BEFORE.bind(this, before.toDate(), maxResults);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID)).collect(Collectors.toList());
        }
    }

    @Override
    public Optional<Conversation> findExistingConversationFor(ConversationIndexKey key) {
        try (Timer.Context ignored = findByIndexKeyTimer.time()) {
            Statement bound = Statements.SELECT_WHERE_COMPOUND_KEY.bind(this, key.serialize());
            ResultSet resultset = session.execute(bound);
            return Optional
                    .fromNullable(resultset.one())
                    .transform(row -> row.getString(FIELD_CONVERSATION_ID))
                    .transform(this::getById);
        }
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        if (toBeCommittedEvents.isEmpty()) throw new IllegalArgumentException("toBeCommittedEvents must not be empty");
        try (Timer.Context ignored = commitTimer.time()) {
            storeSecretIfNewlyCreated(toBeCommittedEvents);
            storeMetadata(conversationId, toBeCommittedEvents);
            BatchStatement batch = new BatchStatement();
            for (ConversationEvent conversationEvent : toBeCommittedEvents) {
                try {
                    batch.add(Statements.INSERT_CONVERSATION_EVENTS.bind(
                            this,
                            conversationId,
                            conversationEvent.getClass().getCanonicalName() + "@@" + objectMapper.writeValueAsString(conversationEvent)));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Unexpected serialization exception", e);
                }
            }
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    private void storeMetadata(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        // Lookup the compound key (optional) and the last modified date
        Date modifiedDate = null;
        ConversationIndexKey compoundKey = null;
        for (ConversationEvent e : toBeCommittedEvents) {
            if (e instanceof ConversationCreatedEvent) {
                ConversationCreatedEvent createdEvent = ((ConversationCreatedEvent) e);
                if (createdEvent.getState() != ConversationState.DEAD_ON_ARRIVAL) {
                    compoundKey = new ConversationIndexKey(createdEvent.getBuyerId(), createdEvent.getSellerId(), createdEvent.getAdId());
                }
            }
            Date eventDate = e.getConversationModifiedAt().toDate();
            if (modifiedDate == null || modifiedDate.before(eventDate)) {
                modifiedDate = eventDate;
            }
        }
        // assert modifiedDate != null;

        BatchStatement batch = new BatchStatement();
        if (compoundKey != null) {
            // It's a new conversation.
            batch.add(Statements.INSERT_MODIFICATION_IDX.bind(this, conversationId, modifiedDate));
            batch.add(Statements.INSERT_RESUME_IDX.bind(this, compoundKey.serialize(), conversationId));

        } else {
            // Not a new conversation. Need to update the modified date
            ResultSet resultset = session.execute(Statements.SELECT_MODIFICATION_IDX.bind(this, conversationId));
            resultset.forEach(row -> batch.add(Statements.DELETE_MODIFICATION_IDX_ONE.bind(this, conversationId, row.getDate("modification_date"))));
            batch.add(Statements.INSERT_MODIFICATION_IDX.bind(this, conversationId, modifiedDate));
        }
        batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        session.execute(batch);
    }

    private void storeSecretIfNewlyCreated(List<ConversationEvent> toBeCommittedEvents) {
        for (ConversationEvent e : toBeCommittedEvents) {
            if (e instanceof ConversationCreatedEvent) {
                ConversationCreatedEvent createdEvent = ((ConversationCreatedEvent) e);
                if (createdEvent.getState() != ConversationState.DEAD_ON_ARRIVAL) {
                    BatchStatement batch = new BatchStatement();
                    batch.add(Statements.INSERT_CONVERSATION_SECRET.bind(this, createdEvent.getBuyerSecret(), createdEvent.getConversationId()));
                    batch.add(Statements.INSERT_CONVERSATION_SECRET.bind(this, createdEvent.getSellerSecret(), createdEvent.getConversationId()));
                    batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
                    session.execute(batch);
                }
                return;
            }
        }
    }

    @Override
    public void deleteConversation(Conversation c) {
        try (Timer.Context ignored = deleteTimer.time()) {
            if (c.getBuyerSecret() != null) {
                session.execute(Statements.DELETE_CONVERSATION_SECRET.bind(this, c.getBuyerSecret()));
            }
            if (c.getSellerSecret() != null) {
                session.execute(Statements.DELETE_CONVERSATION_SECRET.bind(this, c.getSellerSecret()));
            }
            if (c.getBuyerId() != null && c.getSellerId() != null && c.getAdId() != null) {
                session.execute(Statements.DELETE_RESUME_IDX.bind(this, new ConversationIndexKey(c.getBuyerId(), c.getSellerId(), c.getAdId()).serialize()));
            }

            BatchStatement batch = new BatchStatement();
            String conversationId = c.getId();
            batch.add(Statements.DELETE_CONVERSATION_EVENTS.bind(this, conversationId));
            // Delete core_conversation_modification_idx last as it is used to find conversation to delete:
            batch.add(Statements.DELETE_MODIFICATION_IDX.bind(this, conversationId));
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

//    /**
//     * Low level access to all events in a given date time range.
//     *
//     * This implementation loads the first page (by default 5000 elements) and will continue to load the next
//     * page as more are requested.
//     *
//     * All events will have the `replay` flag turned on.
//     *
//     * @param start lowest date for which you want events
//     * @param end highest date for which you want events
//     * @return stream of {@link com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent}s
//     */
//    public Stream<ExtendedConversationEvent> findEventsCreatedBetween(DateTime start, DateTime end) {
//        ConversationEventExtension conversationEventExtension = new ConversationEventExtension();
//        Statement statement = Statements.SELECT_EVENTS_WHERE_CREATE_BETWEEN.bind(this, start.toDate(), end.toDate());
//        return toStream(session.execute(statement))
//                .map(conversationEventExtension::rowToExtendedConversationEvent);
//    }
//
//    private class ConversationEventExtension {
//        private String cachedConversationId;
//        private List<ConversationEvent> cachedConversationEvents;
//
//        private ExtendedConversationEvent rowToExtendedConversationEvent(Row row) {
//            String conversationId = row.getString(FIELD_CONVERSATION_ID);
//            UUID eventId = row.getUUID(FIELD_EVENT_ID);
//
//            ConversationEvent conversationEvent = rowToConversationEvent(row);
//            Conversation conversation = conversationFor(conversationId, eventId.toString());
//
//            // TODO: implement!
//            String sellerAnonymousEmail = null;
//            String buyerAnonymousEmail = null;
//
//            return new ExtendedConversationEvent(conversation, conversationEvent, sellerAnonymousEmail, buyerAnonymousEmail, true);
//        }
//
//        private Conversation conversationFor(String conversationId, String eventId) {
//            List<ConversationEvent> allConversationEvents;
//            if (conversationId.equals(cachedConversationEvents)) {
//                allConversationEvents = cachedConversationEvents;
//            } else {
//                allConversationEvents = CassandraConversationRepository.this.getConversationEvents(conversationId);
//                cachedConversationId = conversationId;
//                cachedConversationEvents = allConversationEvents;
//            }
//
//            int positionOfEvent = Iterables.indexOf(allConversationEvents, event -> event.getEventId().equalsIgnoreCase(eventId));
//            List<ConversationEvent> applicableEvents = allConversationEvents.subList(0, positionOfEvent);
//            return replayEvents(applicableEvents);
//        }
//    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {
        SELECT_FROM_CONVERSATION_EVENTS("SELECT * FROM core_conversation_events WHERE conversation_id=? ORDER BY event_id ASC"),
        SELECT_CONVERSATION_ID_FROM_SECRET("SELECT conversation_id FROM core_conversation_secret WHERE secret=? LIMIT 1"),
        SELECT_WHERE_MODIFICATION_BETWEEN("SELECT conversation_id FROM core_conversation_modification_idx WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING"),
        SELECT_WHERE_MODIFICATION_BEFORE("SELECT conversation_id FROM core_conversation_modification_idx WHERE modification_date <= ? LIMIT ? ALLOW FILTERING"),
        SELECT_MODIFICATION_IDX("SELECT conversation_id, modification_date FROM core_conversation_modification_idx WHERE conversation_id=?"),
        SELECT_WHERE_COMPOUND_KEY("SELECT conversation_id FROM core_conversation_resume_idx WHERE compound_key=?"),
        INSERT_CONVERSATION_EVENTS("INSERT INTO core_conversation_events (conversation_id, event_id, event_json) VALUES (?,now(),?)", true),
        INSERT_MODIFICATION_IDX("INSERT INTO core_conversation_modification_idx (conversation_id, modification_date) VALUES (?,?)", true),
        INSERT_RESUME_IDX("INSERT INTO core_conversation_resume_idx (compound_key, conversation_id) VALUES (?,?)", true),
        DELETE_MODIFICATION_IDX_ONE("DELETE FROM core_conversation_modification_idx WHERE conversation_id=? AND modification_date=?", true),
        INSERT_CONVERSATION_SECRET("INSERT INTO core_conversation_secret (secret, conversation_id) VALUES (?,?)", true),
        DELETE_CONVERSATION_EVENTS("DELETE FROM core_conversation_events WHERE conversation_id=?", true),
        DELETE_MODIFICATION_IDX("DELETE FROM core_conversation_modification_idx WHERE conversation_id=?", true),
        DELETE_CONVERSATION_SECRET("DELETE FROM core_conversation_secret WHERE secret=?", true),
        DELETE_RESUME_IDX("DELETE FROM core_conversation_resume_idx WHERE compound_key=?", true);
        // SELECT_EVENTS_WHERE_CREATE_BETWEEN("SELECT * FROM core_conversation_events WHERE event_id > minTimeuuid(?) AND event_id < maxTimeuuid(?) ALLOW FILTERING");

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

        public Statement bind(CassandraConversationRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraConversationRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }
}
