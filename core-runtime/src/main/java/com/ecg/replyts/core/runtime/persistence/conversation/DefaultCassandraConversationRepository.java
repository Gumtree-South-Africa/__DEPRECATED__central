package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.*;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.replay;
import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Cassandra backed conversation repository.
 */
public class DefaultCassandraConversationRepository implements CassandraRepository, CassandraConversationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCassandraConversationRepository.class);

    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_TIMESTAMP = "modification_timestamp";
    private static final String FIELD_EVENT_ID = "event_id";
    private static final String FIELD_EVENT_JSON = "event_json";

    private final Session session;
    private final Map<StatementsBase, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer getByIdTimer = TimingReports.newTimer("cassandra.conversationRepo-getById");
    private final Timer getBySecretTimer = TimingReports.newTimer("cassandra.conversationRepo-getBySecret");
    private final Timer isSecretAvailableTimer = TimingReports.newTimer("cassandra.conversationRepo-isSecretAvailable");
    private final Timer commitTimer = TimingReports.newTimer("cassandra.conversationRepo-commit");
    private final Timer modifiedBetweenTimer = TimingReports.newTimer("cassandra.conversationRepo-modifiedBetween");
    private final Timer streamConversationsModifiedBetweenTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationIds");
    private final Timer streamConversationEventIdxsByHourTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationEventIdxsByHour");
    private final Timer streamConversationEventIndexesByHourTimer = TimingReports.newTimer("cassandra.conversationRepo-streamConversationEventIndexesByHour");
    private final Timer getLastModifiedDate = TimingReports.newTimer("cassandra.conversationRepo-getLastModifiedDate");
    private final Timer getConversationModificationDates = TimingReports.newTimer("cassandra.conversationRepo-getConversationModificationDates");
    private final Timer modifiedBeforeTimer = TimingReports.newTimer("cassandra.conversationRepo-modifiedBefore");
    private final Timer findByIndexKeyTimer = TimingReports.newTimer("cassandra.conversationRepo-findByIndexKey");
    private final Timer deleteTimer = TimingReports.newTimer("cassandra.conversationRepo-delete");
    private final Timer deleteConversationModificationIdxsTimer = TimingReports.newTimer("cassandra.conversationRepo-deleteConversationModificationIdxs");
    private final Histogram committedBatchSizeHistogram = TimingReports.newHistogram("cassandra.conversationRepo-commit-batch-size");

    private final ConversationResumer resumer;

    private final int conversationEventsFetchLimit;

    public DefaultCassandraConversationRepository(Session session, ConsistencyLevel readConsistency,
                                                  ConsistencyLevel writeConsistency, ConversationResumer resumer,
                                                  int conversationEventsFetchLimit) {
        checkArgument(conversationEventsFetchLimit > 0, "conversationEventsFetchLimit must be a strictly positive number");

        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
        this.resumer = resumer;
        this.conversationEventsFetchLimit = conversationEventsFetchLimit;
    }

    @Override
    public MutableConversation getById(String conversationId) {
        try (Timer.Context ignored = getByIdTimer.time()) {
            MDC.put(MDCConstants.CONVERSATION_ID, conversationId);
            List<ConversationEvent> events = getConversationEvents(conversationId);
            LOG.trace("Found {} events for Conversation with id {} in Cassandra", events.size(), conversationId);
            if (events.isEmpty()) {
                return null;
            }
            return new DefaultMutableConversation(replay(events));
        }
    }

    List<ConversationEvent> getConversationEvents(String conversationId) {
        Statement statement = Statements.SELECT_FROM_CONVERSATION_EVENTS.bind(this, conversationId)
                .setFetchSize(conversationEventsFetchLimit);
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
            ConversationEvent conversationEvent = ObjectMapperConfigurer.getObjectMapper().readValue(eventJson, (Class<? extends ConversationEvent>) Class.forName(className));
            conversationEvent.setEventTimeUUID(eventId);
            return conversationEvent;
        } catch (Exception e) {
            String conversationId = row.getString(FIELD_CONVERSATION_ID);
            throw new RuntimeException("Couldn't parse conversation event " + eventId + " in conversation " + conversationId, e);
        }
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

    // https://jira.corp.ebay.com/browse/COMAAS-645 TODO only need data and conversationId here
    // TO BE DELETED WHEN ALL TENANTS ARE ON THE NEW INDEX
    @Override
    public Stream<ConversationEventIdx> streamConversationEventIdxsByHour(DateTime date) {
        try (Timer.Context ignored = streamConversationEventIdxsByHourTimer.time()) {
            DateTime creationDateRoundedByHour = date.hourOfDay().roundFloorCopy();
            Statement bound = Statements.SELECT_CONVERSATION_EVENTS_BY_DATE.bind(this, creationDateRoundedByHour.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new ConversationEventIdx(creationDateRoundedByHour, row.getString(FIELD_CONVERSATION_ID), row.getUUID(FIELD_EVENT_ID)));
        }
    }

    @Override
    public Stream<ConversationEventIndex> streamConversationEventIndexesByHour(DateTime date) {
        try (Timer.Context ignored = streamConversationEventIndexesByHourTimer.time()) {
            DateTime creationDateRoundedByHour = date.hourOfDay().roundFloorCopy();
            Statement bound = Statements.SELECT_CONVERSATIONS_BY_DATE.bind(this, creationDateRoundedByHour.toDate());
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> new ConversationEventIndex(creationDateRoundedByHour, row.getString(FIELD_CONVERSATION_ID)));
        }
    }

    @Override
    public List<String> listConversationsCreatedBetween(DateTime start, DateTime end) {
        throw new UnsupportedOperationException("listConversationsCreatedBetween not available for Cassandra");
    }

    @Override
    public Long getLastModifiedDate(String conversationId) {
        try (Timer.Context ignored = getLastModifiedDate.time()) {
            Statement bound = Statements.SELECT_LATEST_CONVERSATION_MODIFICATION_IDX.bind(this, conversationId);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            if (row == null) {
                return null;
            }
            return row.getLong(FIELD_MODIFICATION_TIMESTAMP);
        }
    }

    @Override
    public void deleteConversationModificationIdxs(String conversationId) {
        try (Timer.Context ignored = deleteConversationModificationIdxsTimer.time()) {
            Statement deleteConversationModificationIdxStatement = Statements.DELETE_CONVERSATION_MODIFICATION_IDXS.bind(this, conversationId);
            session.execute(deleteConversationModificationIdxStatement);
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
                    .ofNullable(resultset.one())
                    .map(row -> row.getString(FIELD_CONVERSATION_ID))
                    .map(this::getById);
        }
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        if (toBeCommittedEvents.isEmpty()) {
            throw new IllegalArgumentException("toBeCommittedEvents must not be empty");
        }

        try (Timer.Context ignored = commitTimer.time()) {
            BatchStatement batch = new BatchStatement();

            storeSecretIfNewlyCreated(batch, toBeCommittedEvents);
            storeMetadata(batch, conversationId, toBeCommittedEvents);

            for (ConversationEvent conversationEvent : toBeCommittedEvents) {
                try {
                    UUID eventId = conversationEvent.getEventTimeUUID();
                    DateTime currentTime = new DateTime(UUIDs.unixTimestamp(eventId));
                    String jsonEventStr = ObjectMapperConfigurer.getObjectMapper().writeValueAsString(conversationEvent);
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
                                eventId)
                        );
                        batch.add(Statements.INSERT_CONVERSATION_BY_DATE.bind(
                                this,
                                currentTime.hourOfDay().roundFloorCopy().toDate(),
                                conversationId)
                        );
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Unexpected serialization exception", e);
                }
            }

            batch.setConsistencyLevel(getWriteConsistency()).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            committedBatchSizeHistogram.update(batch.size());
            LOG.trace("Saving conversation {}, with {} events and batch size {} to Cassandra", conversationId, toBeCommittedEvents.size(), batch.size());

            session.execute(batch);
        }
    }

    private List<Long> getConversationModificationDates(String conversationId) {
        try (Timer.Context ignored = getConversationModificationDates.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_MODIFICATION_IDXS.bind(this, conversationId);
            ResultSet resultset = session.execute(bound);
            return toStream(resultset).map(row -> row.getLong(FIELD_MODIFICATION_TIMESTAMP)).collect(Collectors.toList());
        }
    }

    private void storeMetadata(BatchStatement batch, String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        // Lookup the compound key (optional) and the last modified date
        DateTime modifiedDateTime = getConversationModifiedDateTime(toBeCommittedEvents);
        Date modifiedDate = modifiedDateTime.toDate();
        Optional<ConversationIndexKey> compoundKey = getConversationCompoundKey(toBeCommittedEvents);

        // It's a new conversation.
        compoundKey.ifPresent(conversationIndexKey -> batch.add(Statements.INSERT_RESUME_IDX.bind(this, conversationIndexKey.serialize(), conversationId)));

        batch.add(Statements.INSERT_CONVERSATION_MODIFICATION_IDX.bind(this, conversationId, modifiedDate));
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

    private Optional<ConversationIndexKey> getConversationCompoundKey(List<ConversationEvent> toBeCommittedEvents) {
        return toBeCommittedEvents.stream()
                .filter((e) -> e instanceof ConversationCreatedEvent && ((ConversationCreatedEvent) e).getState() != ConversationState.DEAD_ON_ARRIVAL)
                .map((e) -> resumer.keyFromCreatedEvent((ConversationCreatedEvent) e))
                .findFirst();
    }

    private void storeSecretIfNewlyCreated(BatchStatement batch, List<ConversationEvent> toBeCommittedEvents) {
        for (ConversationEvent e : toBeCommittedEvents) {
            if (e instanceof ConversationCreatedEvent) {
                ConversationCreatedEvent createdEvent = ((ConversationCreatedEvent) e);
                if (createdEvent.getState() != ConversationState.DEAD_ON_ARRIVAL) {
                    batch.add(Statements.INSERT_CONVERSATION_SECRET.bind(this, createdEvent.getBuyerSecret(), createdEvent.getConversationId()));
                    batch.add(Statements.INSERT_CONVERSATION_SECRET.bind(this, createdEvent.getSellerSecret(), createdEvent.getConversationId()));
                }
                return;
            }
        }
    }

    @Override
    public void deleteConversation(Conversation c) {
        String conversationId = c.getId();
        List<Long> conversationModificationDates = getConversationModificationDates(conversationId);
        try (Timer.Context ignored = deleteTimer.time()) {
            if (c.getBuyerSecret() != null) {
                session.execute(Statements.DELETE_CONVERSATION_SECRET.bind(this, c.getBuyerSecret()));
            }
            if (c.getSellerSecret() != null) {
                session.execute(Statements.DELETE_CONVERSATION_SECRET.bind(this, c.getSellerSecret()));
            }
            if (c.getBuyerId() != null && c.getSellerId() != null && c.getAdId() != null) {
                session.execute(Statements.DELETE_RESUME_IDX.bind(this, resumer.keyFromConversation(c).serialize()));
            }

            BatchStatement batch = new BatchStatement();
            batch.add(Statements.DELETE_CONVERSATION_EVENTS.bind(this, conversationId));
            // Delete core_conversation_modification_desc_idx after conversation event as it is used to find conversation to delete:
            batch.add(Statements.DELETE_CONVERSATION_MODIFICATION_IDXS.bind(this, conversationId));
            // Delete core_conversation_events_by_date last as it is used to find core_conversation_modification_desc_idx
            conversationModificationDates.forEach(conversationModificationDate -> {
                Date roundedDate = new DateTime(conversationModificationDate).hourOfDay().roundFloorCopy().toDate();
                batch.add(Statements.DELETE_CONVERSATION_EVENTS_BY_DATE.bind(this, roundedDate, conversationId));
                batch.add(Statements.DELETE_CONVERSATION_BY_DATE.bind(this, roundedDate, conversationId));
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
            if (applicableEvents.isEmpty()) {
                return null;
            }
            return new DefaultMutableConversation(replay(applicableEvents));
        }
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
        static Statements SELECT_FROM_CONVERSATION_EVENTS = new Statements("SELECT * FROM core_conversation_events WHERE conversation_id=? ORDER BY event_id ASC", false);
        static Statements SELECT_CONVERSATION_EVENTS_BY_DATE = new Statements("SELECT conversation_id, event_id FROM core_conversation_events_by_date WHERE creatdate = ?", false);
        static Statements SELECT_CONVERSATIONS_BY_DATE = new Statements("SELECT conversation_id FROM core_conversation_events_index_by_date WHERE rounded_event_date = ?", false);
        static Statements SELECT_CONVERSATION_ID_FROM_SECRET = new Statements("SELECT conversation_id FROM core_conversation_secret WHERE secret=? LIMIT 1", false);
        static Statements SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN = new Statements("SELECT conversation_id FROM core_conversation_modification_desc_idx WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING", false);
        static Statements SELECT_CONVERSATION_WHERE_MODIFICATION_BEFORE = new Statements("SELECT conversation_id FROM core_conversation_modification_desc_idx WHERE modification_date <= ? LIMIT ? ALLOW FILTERING", false);
        static Statements SELECT_CONVERSATION_MODIFICATION_IDXS = new Statements("SELECT conversation_id, modification_date, blobAsBigint(timestampAsBlob(modification_date)) as modification_timestamp FROM core_conversation_modification_desc_idx WHERE conversation_id = ?", false);
        static Statements SELECT_LATEST_CONVERSATION_MODIFICATION_IDX = new Statements("SELECT conversation_id, modification_date, blobAsBigint(timestampAsBlob(modification_date)) as modification_timestamp FROM core_conversation_modification_desc_idx WHERE conversation_id = ? LIMIT 1", false);
        static Statements SELECT_WHERE_COMPOUND_KEY = new Statements("SELECT conversation_id FROM core_conversation_resume_idx WHERE compound_key=?", false);

        static Statements INSERT_CONVERSATION_EVENTS = new Statements("INSERT INTO core_conversation_events (conversation_id, event_id, event_json) VALUES (?,?,?)", true);
        static Statements INSERT_CONVERSATION_SECRET = new Statements("INSERT INTO core_conversation_secret (secret, conversation_id) VALUES (?,?)", true);
        static Statements INSERT_CONVERSATION_MODIFICATION_IDX = new Statements("INSERT INTO core_conversation_modification_desc_idx (conversation_id, modification_date) VALUES (?,?)", true);
        static Statements INSERT_RESUME_IDX = new Statements("INSERT INTO core_conversation_resume_idx (compound_key, conversation_id) VALUES (?,?)", true);
        static Statements INSERT_CONVERSATION_EVENTS_BY_DATE = new Statements("INSERT INTO core_conversation_events_by_date (creatdate, conversation_id, event_id) VALUES (?, ?, ?)", true);
        static Statements INSERT_CONVERSATION_BY_DATE = new Statements("INSERT INTO core_conversation_events_index_by_date (rounded_event_date, conversation_id) VALUES (?, ?)", true);

        static Statements DELETE_CONVERSATION_EVENTS = new Statements("DELETE FROM core_conversation_events WHERE conversation_id=?", true);
        static Statements DELETE_CONVERSATION_SECRET = new Statements("DELETE FROM core_conversation_secret WHERE secret=?", true);
        static Statements DELETE_CONVERSATION_MODIFICATION_IDXS = new Statements("DELETE FROM core_conversation_modification_desc_idx WHERE conversation_id=?", true);
        static Statements DELETE_CONVERSATION_EVENTS_BY_DATE = new Statements("DELETE FROM core_conversation_events_by_date WHERE creatdate = ? AND conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_BY_DATE = new Statements("DELETE FROM core_conversation_events_index_by_date WHERE rounded_event_date = ? AND conversation_id = ?", true);
        static Statements DELETE_RESUME_IDX = new Statements("DELETE FROM core_conversation_resume_idx WHERE compound_key=?", true);

        static Statements SELECT_EVENTS_WHERE_CREATE_BETWEEN = new Statements("SELECT * FROM core_conversation_events WHERE event_id > minTimeuuid(?) AND event_id < maxTimeuuid(?) ALLOW FILTERING", false);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }
}
