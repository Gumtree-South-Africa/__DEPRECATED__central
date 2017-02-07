package com.ecg.comaas.r2cmigration.difftool.repo;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

@Repository
public class CassConversationRepo {

    private static final Logger LOG = LoggerFactory.getLogger(CassConversationRepo.class);
    private final static Timer GET_BY_ID_CASS_TIMER = TimingReports.newTimer("cass-getById");

    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_EVENT_ID = "event_id";
    private static final String FIELD_EVENT_JSON = "event_json";

    private static final String COUNT_FROM_CONVERSATION_MOD_IDX = "SELECT count(*) FROM core_conversation_modification_desc_idx WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";
    private static final String COUNT_FROM_CONVERSATION_MOD_IDX_BY_DAY = "SELECT count(*) FROM core_conversation_modification_desc_idx_by_day WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";
    private static final String SELECT_FROM_CONVERSATION_EVENTS = "SELECT * FROM core_conversation_events WHERE conversation_id=? ORDER BY event_id ASC";
    private static final String SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN = "SELECT conversation_id FROM core_conversation_modification_desc_idx WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";

    private final ObjectMapper objectMapper;
    private final PreparedStatement getByConvID;
    private final PreparedStatement getByDate;
    private final PreparedStatement getCount;
    private final PreparedStatement getCountByDay;

    private Session session;

    private final ConsistencyLevel cassandraReadConsistency;
    private final ConsistencyLevel cassandraWriteConsistency;

    @Autowired
    public CassConversationRepo(@Qualifier("cassandraSession") Session session,
                                JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer,
                                @Value("${persistence.cassandra.consistency.read:#{null}}") ConsistencyLevel cassandraReadConsistency,
                                @Value("${persistence.cassandra.consistency.write:#{null}}") ConsistencyLevel cassandraWriteConsistency) {
        try {
            this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
            this.session = session;
            this.getByConvID = session.prepare(SELECT_FROM_CONVERSATION_EVENTS);
            this.getByDate = session.prepare(SELECT_CONVERSATION_WHERE_MODIFICATION_BETWEEN);
            this.getCount = session.prepare(COUNT_FROM_CONVERSATION_MOD_IDX);
            this.getCountByDay = session.prepare(COUNT_FROM_CONVERSATION_MOD_IDX_BY_DAY);
            this.cassandraReadConsistency = cassandraReadConsistency;
            this.cassandraWriteConsistency = cassandraWriteConsistency;
        } catch (Exception e) {
            LOG.error("Fail to connect to cassandra: ", e);
            throw new RuntimeException(e);
        }
    }

    public List<ConversationEvent> getById(String conversationId) {
        return getConversationEvents(conversationId);
    }

    private List<ConversationEvent> getConversationEvents(String conversationId) {
        try (Timer.Context ignored = GET_BY_ID_CASS_TIMER.time()) {
            Statement statement = bind(getByConvID, conversationId);
            ResultSet resultset = session.execute(statement);
            return toStream(resultset)
                    .map(this::rowToConversationEvent)
                    .collect(Collectors.toList());
        }
    }

    public Statement bind(PreparedStatement statement, Object... values) {
        BoundStatement bs = statement.bind(values);
        return bs.setConsistencyLevel(cassandraReadConsistency).setSerialConsistencyLevel(cassandraWriteConsistency);
    }

    public long getConversationModCount(DateTime startDate, DateTime endDate) {
        Statement statement = bind(getCount, startDate.toDate(), endDate.toDate());
        ResultSet resultset = session.execute(statement);
        return resultset.one().getLong("count");
    }

    public long getConversationModByDayCount(DateTime startDate, DateTime endDate) {
        Statement statement = bind(getCountByDay, startDate.toDate(), endDate.toDate());
        LOG.debug("getConversationModByDayCount {} - {} ", startDate.toDate(), endDate.toDate());
        ResultSet resultset = session.execute(statement);
        return resultset.one().getLong("count");
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

    private Map.Entry<String, List<ConversationEvent>> getConversationEvents(Row row) {
        String conversationId = row.getString(FIELD_CONVERSATION_ID);
        return new AbstractMap.SimpleImmutableEntry<String, List<ConversationEvent>>(conversationId, getConversationEvents(conversationId));
    }

    public Stream<Map.Entry<String, List<ConversationEvent>>> findEventsModifiedBetween(DateTime start, DateTime end) {
        Statement statement = getByDate.bind(start.toDate(), end.toDate());
        return toStream(session.execute(statement)).map(this::getConversationEvents);
    }
}
