package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventId;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

/**
 * Cassandra backed conversation repository.
 */
public class DefaultCassandraConversationRepository implements CassandraConversationRepository {

    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_DATE = "modification_date";
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
    private final Timer streamConversationsModifiedBetweenTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationIds");
    private final Timer streamConversationEventIdsByDayTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationEventIds");
    private final Timer streamConversationModificationsByDayTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationModificationsByDay");
    private final Timer getLastModifiedDate = TimingReports.newTimer("cassandra.conversationRepo-getLastModifiedDate");
    private final Timer getConversationModificationDates = TimingReports.newTimer("cassandra.conversationRepo-getConversationModificationDates");
    private final Timer modifiedBeforeTimer = TimingReports.newTimer("cassandra.conversationRepo-modifiedBefore");
    private final Timer findByIndexKeyTimer = TimingReports.newTimer("cassandra.conversationRepo-findByIndexKey");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.conversationRepo-delete");
    private final Timer deleteOldModificationDateTimer = TimingReports.newTimer("cassandra.conversationRepo-deleteOldModificationDate");

    private ObjectMapper objectMapper;

    public DefaultCassandraConversationRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
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
        UUID eventId = row.getUUID(FIELD_EVENT_ID);
        String eventJsonWithClass = row.getString(FIELD_EVENT_JSON);
        String className = eventJsonWithClass.substring(0, eventJsonWithClass.indexOf("@@"));
        String eventJson = eventJsonWithClass.substring(eventJsonWithClass.indexOf("@@") + 2);
        try {
            ConversationEvent conversationEvent = objectMapper.readValue(eventJson, (Class<? extends ConversationEvent>) Class.forName(className));
            conversationEvent.setEventTimeUUID(eventId);
            return conversationEvent;
        } catch (Exception e) {
            String conversationId = row.getString(FIELD_CONVERSATION_ID);
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
            Statement bound = Statements.SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN.bind(this, start.toDate(), end.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID)).collect(Collectors.toList());
        }
    }

    @Override
    public Stream<String> streamConversationsModifiedBetween(DateTime start, DateTime end) {
        try (Timer.Context ignored = streamConversationsModifiedBetweenTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN.bind(this, start.toDate(), end.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID));
        }
    }

    @Override
    public Stream<ConversationModificationDate> streamConversationModificationsByDay(int year, int month, int day) {
        try (Timer.Context ignored = streamConversationModificationsByDayTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_MODIFICATION_IDX_BY_YEAR_MONTH_DAY.bind(this, year, month, day);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new ConversationModificationDate(row.getString(FIELD_CONVERSATION_ID), row.getDate(FIELD_MODIFICATION_DATE)));
        }
    }

    public Stream<ConversationEventId> streamConversationEventIdsByHour(DateTime date) {
        try (Timer.Context ignored = streamConversationEventIdsByDayTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_EVENTS_BY_DATE.bind(this, date.hourOfDay().roundFloorCopy().toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new ConversationEventId(row.getString(FIELD_CONVERSATION_ID), row.getUUID(FIELD_EVENT_ID)));
        }
    }

    @Override
    public List<String> listConversationsCreatedBetween(DateTime start, DateTime end) {
        throw new UnsupportedOperationException("listConversationsCreatedBetween not available for Cassandra");
    }

    @Override
    public DateTime getLastModifiedDate(String conversationId) {
        try (Timer.Context ignored = getLastModifiedDate.time()) {
            Statement bound = Statements.SELECT_LATEST_CONVERSATION_MODIFICATION_IDX.bind(this, conversationId);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            if (row == null) {
                return null;
            }
            return new DateTime(row.getDate(FIELD_MODIFICATION_DATE));
        }
    }

    @Override
    public void deleteOldConversationModificationDate(ConversationModificationDate conversationModificationDate) {
        try (Timer.Context ignored = deleteOldModificationDateTimer.time()) {
            BatchStatement batch = new BatchStatement();
            DateTime conversationModificationDateTime = conversationModificationDate.getModificationDateTime();
            batch.add(Statements.DELETE_CONVERSATION_MODIFICATION_IDX.bind(this, conversationModificationDate.getConversationId(), conversationModificationDate.getModificationDate()));
            batch.add(Statements.DELETE_CONVERSATION_MODIFICATION_IDX_BY_DAY.bind(this, conversationModificationDateTime.getYear(), conversationModificationDateTime.getMonthOfYear(),
                    conversationModificationDateTime.getDayOfMonth(), conversationModificationDate.getModificationDate(), conversationModificationDate.getConversationId()));
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    @Override
    public Set<String> getConversationsModifiedBefore(DateTime before, int maxResults) {
        try (Timer.Context ignored = modifiedBeforeTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_WHERE_MODIFICATION_BEFORE.bind(this, before.toDate(), maxResults);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getString(FIELD_CONVERSATION_ID)).collect(Collectors.toSet());
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
                    UUID eventId = conversationEvent.getEventTimeUUID().get();
                    DateTime currentTime = new DateTime(UUIDs.unixTimestamp(eventId));
                    String jsonEventStr = objectMapper.writeValueAsString(conversationEvent);
                    batch.add(Statements.INSERT_CONVERSATION_EVENTS.bind(
                            this,
                            conversationId,
                            eventId,
                            conversationEvent.getClass().getCanonicalName() + "@@" + jsonEventStr));
                    if (conversationEvent instanceof MessageAddedEvent) {
                        batch.add(Statements.INSERT_CONVERSATION_EVENTS_BY_DATE.bind(
                                this,
                                currentTime.hourOfDay().roundFloorCopy().toDate(),
                                conversationId,
                                eventId,
                                jsonEventStr)
                        );
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Unexpected serialization exception", e);
                }
            }
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    private List<ConversationModificationDate> getConversationModificationDates(String conversationId) {
        try (Timer.Context ignored = getConversationModificationDates.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_MODIFICATION_IDXS.bind(this, conversationId);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new ConversationModificationDate(row.getString(FIELD_CONVERSATION_ID), row.getDate(FIELD_MODIFICATION_DATE)))
                    .collect(Collectors.toList());
        }
    }

    private void storeMetadata(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        // Lookup the compound key (optional) and the last modified date
        DateTime modifiedDateTime = getConversationModifiedDateTime(toBeCommittedEvents);
        Date modifiedDate = modifiedDateTime.toDate();
        ConversationIndexKey compoundKey = getConversationCompoundKey(toBeCommittedEvents);

        BatchStatement batch = new BatchStatement();
        if (compoundKey != null) {
            // It's a new conversation.
            batch.add(Statements.INSERT_RESUME_IDX.bind(this, compoundKey.serialize(), conversationId));
        }

        batch.add(Statements.INSERT_CONVERSATION_MODIFICATION_IDX.bind(this, conversationId, modifiedDate));
        batch.add(Statements.INSERT_CONVERSATION_MODIFICATION_IDX_BY_DAY.bind(this, modifiedDateTime.getYear(), modifiedDateTime.getMonthOfYear(),
                modifiedDateTime.getDayOfMonth(), modifiedDate, conversationId));

        batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        session.execute(batch);
    }

    private DateTime getConversationModifiedDateTime(List<ConversationEvent> toBeCommittedEvents) {
        DateTime modifiedDateTime = null;
        for (ConversationEvent e : toBeCommittedEvents) {
            DateTime eventDateTime = e.getConversationModifiedAt();
            if (modifiedDateTime == null || modifiedDateTime.isBefore(eventDateTime)) {
                modifiedDateTime = eventDateTime;
            }
        }
        return modifiedDateTime;
    }

    private ConversationIndexKey getConversationCompoundKey(List<ConversationEvent> toBeCommittedEvents) {
        ConversationIndexKey compoundKey = null;
        for (ConversationEvent e : toBeCommittedEvents) {
            if (e instanceof ConversationCreatedEvent) {
                ConversationCreatedEvent createdEvent = ((ConversationCreatedEvent) e);
                if (createdEvent.getState() != ConversationState.DEAD_ON_ARRIVAL) {
                    compoundKey = new ConversationIndexKey(createdEvent.getBuyerId(), createdEvent.getSellerId(), createdEvent.getAdId());
                }
            }
        }
        return compoundKey;
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
        String conversationId = c.getId();
        List<ConversationModificationDate> conversationModificationDates = getConversationModificationDates(conversationId);
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
            batch.add(Statements.DELETE_CONVERSATION_EVENTS.bind(this, conversationId));
            // Delete core_conversation_modification_desc_idx after conversation event as it is used to find conversation to delete:
            batch.add(Statements.DELETE_CONVERSATION_MODIFICATION_IDXS.bind(this, conversationId));
            // Delete core_conversation_modification_desc_idx_by_day last as it is used to find core_conversation_modification_desc_idx
            conversationModificationDates.forEach(conversationModificationDate -> {
                DateTime modificationDateTime = conversationModificationDate.getModificationDateTime();
                batch.add(Statements.DELETE_CONVERSATION_MODIFICATION_IDX_BY_DAY.bind(this, modificationDateTime.getYear(), modificationDateTime.getMonthOfYear(),
                        modificationDateTime.getDayOfMonth(), conversationModificationDate.getModificationDate(), conversationId));
                batch.add(Statements.DELETE_CONVERSATION_EVENTS_BY_DATE.bind(this, modificationDateTime.hourOfDay().roundFloorCopy().toDate(), conversationId));

            });
            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
            session.execute(batch);
        }
    }

    @Override
    public Stream<ImmutablePair<Conversation, ConversationEvent>> findEventsCreatedBetween(DateTime start, DateTime end) {
        ConversationEventExtension conversationEventExtension = new ConversationEventExtension();
        Statement statement = Statements.SELECT_EVENTS_WHERE_CREATE_BETWEEN.bind(this, start.toDate(), end.toDate());
        return toStream(session.execute(statement))
                .map(conversationEventExtension::rowToConversationEventTuple);
    }

    private class ConversationEventExtension {

        private String cachedConversationId;
        private List<ConversationEvent> cachedConversationEvents;

        private ImmutablePair<Conversation, ConversationEvent> rowToConversationEventTuple(Row row) {
            String conversationId = row.getString(FIELD_CONVERSATION_ID);

            ConversationEvent conversationEvent = rowToConversationEvent(row);
            Conversation conversation = conversationFor(conversationId, conversationEvent.getEventId());

            return new ImmutablePair<>(conversation, conversationEvent);
        }

        private Conversation conversationFor(String conversationId, String eventId) {
            // Optimization: while streaming we will mostly encounter 3 or 4 events in a row for the same conversation id.
            // Caching the events for the last fetched conversation id will reduce the number of fetches.
            if (!conversationId.equals(cachedConversationId)) {
                cachedConversationId = conversationId;
                cachedConversationEvents = getConversationEvents(conversationId);
            }

            int positionOfEvent = Iterables.indexOf(cachedConversationEvents, event -> event.getEventId().equalsIgnoreCase(eventId));
            List<ConversationEvent> applicableEvents = cachedConversationEvents.subList(0, positionOfEvent + 1);
            return replayEvents(applicableEvents);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {

        SELECT_FROM_CONVERSATION_EVENTS("SELECT * FROM core_conversation_events WHERE conversation_id=? ORDER BY event_id ASC"),
        SELECT_CONVERSATION_EVENTS_BY_DATE("SELECT conversation_id, event_id FROM core_conversation_events_by_date WHERE creatdate = ?"),
        SELECT_CONVERSATION_ID_FROM_SECRET("SELECT conversation_id FROM core_conversation_secret WHERE secret=? LIMIT 1"),
        SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN("SELECT conversation_id FROM core_conversation_modification_desc_idx WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING"),
        SELECT_CONVERSATION_WHERE_MODIFICATION_BEFORE("SELECT conversation_id FROM core_conversation_modification_desc_idx WHERE modification_date <= ? LIMIT ? ALLOW FILTERING"),
        SELECT_CONVERSATION_MODIFICATION_IDX("SELECT conversation_id, modification_date FROM core_conversation_modification_desc_idx WHERE conversation_id = ? AND modification_date = ?"),
        SELECT_CONVERSATION_MODIFICATION_IDXS("SELECT conversation_id, modification_date FROM core_conversation_modification_desc_idx WHERE conversation_id = ?"),
        SELECT_LATEST_CONVERSATION_MODIFICATION_IDX("SELECT conversation_id, modification_date FROM core_conversation_modification_desc_idx WHERE conversation_id = ? LIMIT 1"),
        SELECT_CONVERSATION_MODIFICATION_IDX_BY_DAY("SELECT conversation_id, modification_date FROM core_conversation_modification_desc_idx_by_day WHERE year = ? AND month = ? AND day = ? AND modification_date = ? AND conversation_id = ?"),
        SELECT_CONVERSATION_MODIFICATION_IDX_BY_YEAR_MONTH_DAY("SELECT conversation_id, modification_date FROM core_conversation_modification_desc_idx_by_day WHERE year = ? AND month = ? AND day = ?"),
        SELECT_WHERE_COMPOUND_KEY("SELECT conversation_id FROM core_conversation_resume_idx WHERE compound_key=?"),

        INSERT_CONVERSATION_EVENTS("INSERT INTO core_conversation_events (conversation_id, event_id, event_json) VALUES (?,?,?)", true),
        INSERT_CONVERSATION_SECRET("INSERT INTO core_conversation_secret (secret, conversation_id) VALUES (?,?)", true),
        INSERT_CONVERSATION_MODIFICATION_IDX("INSERT INTO core_conversation_modification_desc_idx (conversation_id, modification_date) VALUES (?,?)", true),
        INSERT_CONVERSATION_MODIFICATION_IDX_BY_DAY("INSERT INTO core_conversation_modification_desc_idx_by_day (year, month, day, modification_date, conversation_id) VALUES (?, ?, ?, ?, ?)", true),
        INSERT_RESUME_IDX("INSERT INTO core_conversation_resume_idx (compound_key, conversation_id) VALUES (?,?)", true),
        INSERT_CONVERSATION_EVENTS_BY_DATE("INSERT INTO core_conversation_events_by_date (creatdate, conversation_id, event_id, event_json) VALUES (?, ?, ?, ?)", true),

        DELETE_CONVERSATION_EVENTS("DELETE FROM core_conversation_events WHERE conversation_id=?", true),
        DELETE_CONVERSATION_SECRET("DELETE FROM core_conversation_secret WHERE secret=?", true),
        DELETE_CONVERSATION_MODIFICATION_IDX("DELETE FROM core_conversation_modification_desc_idx WHERE conversation_id = ? AND modification_date = ?", true),
        DELETE_CONVERSATION_MODIFICATION_IDXS("DELETE FROM core_conversation_modification_desc_idx WHERE conversation_id=?", true),
        DELETE_CONVERSATION_MODIFICATION_IDX_BY_DAY("DELETE FROM core_conversation_modification_desc_idx_by_day WHERE year = ? AND month = ? AND day = ? AND modification_date = ? AND conversation_id = ?", true),
        DELETE_CONVERSATION_EVENTS_BY_DATE("DELETE FROM core_conversation_events_by_date WHERE creatdate = ? AND conversation_id = ?", true),
        DELETE_RESUME_IDX("DELETE FROM core_conversation_resume_idx WHERE compound_key=?", true),

        SELECT_EVENTS_WHERE_CREATE_BETWEEN("SELECT * FROM core_conversation_events WHERE event_id > minTimeuuid(?) AND event_id < maxTimeuuid(?) ALLOW FILTERING");

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

        public Statement bind(DefaultCassandraConversationRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(DefaultCassandraConversationRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }
}