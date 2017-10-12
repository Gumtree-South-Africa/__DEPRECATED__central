package com.ecg.comaas.r2cmigration.difftool.repo;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.util.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newGauge;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

@Repository
public class CassPostboxRepo {
    private static final Logger LOG = LoggerFactory.getLogger(CassPostboxRepo.class);

    private static final String SELECT_POSTBOX_Q = "SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id = ?";
    private static final String SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS_Q = "SELECT conversation_id, num_unread FROM mb_unread_counters WHERE postbox_id = ?";
    private static final String SELECT_POSTBOX_WHERE_MODIFICATION_BETWEEN = "SELECT postbox_id FROM mb_postbox_modification_idx_by_date WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";
    private static final String COUNT_POSTBOX_WHERE_MODIFICATION_BETWEEN = "SELECT count(*) FROM mb_postbox_modification_idx_by_date WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";

    private static final String FIELD_POSTBOX_ID = "postbox_id";
    private final Session session;

    private final Timer byIdTimer = newTimer("cassandra.postBoxRepo-byId");
    private AtomicLong streamGauge = new AtomicLong();
    private ObjectMapper objectMapper;

    @Value("${replyts.cleanup.conversation.streaming.queue.size:100000}")
    private int workQueueSize;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int threadCount;

    @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
    private int batchSize;

    PreparedStatement selectPostbox = null;
    PreparedStatement selectPostboxUnreadCount = null;

    PreparedStatement selectPostboxModifiedBetweenByDate = null;
    PreparedStatement countPostboxes = null;

    private final ConsistencyLevel cassandraReadConsistency;

    public CassPostboxRepo(@Qualifier("cassandraSessionForMb") Session session,
                           @Value("${persistence.cassandra.consistency.read:#{null}}") ConsistencyLevel cassandraReadConsistency) {
        this.session = session;
        this.selectPostbox = session.prepare(SELECT_POSTBOX_Q);
        this.selectPostboxUnreadCount = session.prepare(SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS_Q);
        this.selectPostboxModifiedBetweenByDate = session.prepare(SELECT_POSTBOX_WHERE_MODIFICATION_BETWEEN);
        this.countPostboxes = session.prepare(COUNT_POSTBOX_WHERE_MODIFICATION_BETWEEN);
        this.cassandraReadConsistency = cassandraReadConsistency;
        newGauge("cassandra.postboxRepo-streamConversationModificationsByHour", () -> streamGauge.get());
    }

    public PostBox getById(String email) {
        try (Timer.Context ignored = byIdTimer.time()) {
            ResultSet resultSet = session.execute(bind(selectPostboxUnreadCount, email));

            Map<String, Integer> conversationUnreadCounts = StreamUtils.toStream(resultSet).collect(Collectors.toMap(
                    row -> row.getString("conversation_id"),
                    row -> row.getInt("num_unread"))
            );

            List<AbstractConversationThread> conversationThreads = new ArrayList<>();
            AtomicLong newRepliesCount = new AtomicLong();

            ResultSet result = session.execute(bind(selectPostbox, email));

            result.forEach(row -> {
                String conversationId = row.getString("conversation_id");
                String jsonValue = row.getString("json_value");
                int unreadCount = conversationUnreadCounts.getOrDefault(conversationId, 0);

                newRepliesCount.addAndGet(unreadCount);
                Optional<AbstractConversationThread> ctOptional = toConversationThread(
                        email,
                        conversationId,
                        jsonValue,
                        unreadCount);

                ctOptional.map(conversationThreads::add);
            });
            return new PostBox(email, Optional.of(newRepliesCount.get()), conversationThreads);
        }
    }

    public Statement bind(PreparedStatement statement, Object... values) {
        BoundStatement bs = statement.bind(values);
        return bs.setConsistencyLevel(cassandraReadConsistency);
    }

    private Optional<AbstractConversationThread> toConversationThread(String postboxId, String
            conversationId, String jsonValue, int numUnreadMessages) {
        try {
            // AbstractConversationThread is parameterized so store the effective class in the data

            String jsonClass = jsonValue.substring(0, jsonValue.indexOf("@@"));
            String jsonContent = jsonValue.substring(jsonValue.indexOf("@@") + 2);

            AbstractConversationThread conversationThread = objectMapper.readValue(jsonContent, (Class<? extends AbstractConversationThread>) Class.forName(jsonClass));

            conversationThread.setContainsUnreadMessages(numUnreadMessages > 0);

            return Optional.of(conversationThread);
        } catch (ClassNotFoundException | IOException e) {
            LOG.error("Could not deserialize post box {} conversation {} json: {}", postboxId, conversationId, jsonValue, e);

            return Optional.empty();
        }
    }

    // This does not really give any info on how many postboxes there is in the time slice
    // but it does give some ideas how many we are going to pass through.
    public long getPostboxModificationCountByQuery(Date fromDate, Date toDate) {
        Statement bound = bind(countPostboxes, fromDate, toDate);
        ResultSet resultset = session.execute(bound);
        Row row = resultset.one();
        return row.getLong(0);
    }

    public Stream<String> streamMessageBoxIds(Date fromDate, Date toDate) {
        Statement bound = bind(selectPostboxModifiedBetweenByDate, fromDate, toDate);
        ResultSet resultset = session.execute(bound);
        return toStream(resultset).map(row -> row.getString(FIELD_POSTBOX_ID));

    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }

}
