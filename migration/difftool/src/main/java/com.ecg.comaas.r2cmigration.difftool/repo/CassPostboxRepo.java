package com.ecg.comaas.r2cmigration.difftool.repo;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.simple.*;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.util.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

public class CassPostboxRepo {
    private static final Logger LOG = LoggerFactory.getLogger(CassPostboxRepo.class);

    private static final String SELECT_POSTBOX_Q = "SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id = ?";
    public static final String SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS_Q = "SELECT conversation_id, num_unread FROM mb_unread_counters WHERE postbox_id = ?";
    public static final String SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE_Q = "SELECT postbox_id, conversation_id, modification_date, rounded_modification_date FROM mb_postbox_modification_idx_by_date WHERE rounded_modification_date = ?";
    private static final String SELECT_POSTBOX_WHERE_MODIFICATION_BETWEEN = "SELECT postbox_id, conversation_id, modification_date FROM mb_postbox_modification_idx_by_date WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";
    private static final String COUNT_POSTBOX_WHERE_MODIFICATION_BETWEEN = "SELECT count(*) FROM mb_postbox_modification_idx_by_date WHERE modification_date >=? AND modification_date <= ? ALLOW FILTERING";


    private static final String FIELD_POSTBOX_ID = "postbox_id";
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_DATE = "modification_date";
    private static final String FIELD_ROUNDED_MODIFICATION_DATE = "rounded_modification_date";

    private final Session session;

    private final Timer byIdTimer = newTimer("cassandra.postBoxRepo-byId");
    private AtomicLong streamGauge = new AtomicLong();

    private ObjectMapper objectMapper;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @Value("${replyts.cleanup.conversation.streaming.queue.size:100000}")
    private int workQueueSize;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int threadCount;

    @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
    private int batchSize;

    private ThreadPoolExecutor threadPoolExecutor;


    PreparedStatement selectPostbox = null;
    PreparedStatement selectPostboxUnreadCount = null;
    PreparedStatement selectConvThreadIdxByDate = null;
    PreparedStatement selectPostboxModifiedBetweenByDate = null;
    PreparedStatement countPostboxes = null;

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
    }

    public CassPostboxRepo(Session session) {
        this.session = session;
        this.selectPostbox = session.prepare(SELECT_POSTBOX_Q);
        this.selectPostboxUnreadCount = session.prepare(SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS_Q);
        this.selectConvThreadIdxByDate = session.prepare(SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE_Q);
        this.selectPostboxModifiedBetweenByDate = session.prepare(SELECT_POSTBOX_WHERE_MODIFICATION_BETWEEN);
        this.countPostboxes = session.prepare(COUNT_POSTBOX_WHERE_MODIFICATION_BETWEEN);
        newGauge("cassandra.postboxRepo-streamConversationModificationsByHour", () -> streamGauge.get());
    }

    public PostBox getById(String email) {
        try (Timer.Context ignored = byIdTimer.time()) {
            ResultSet resultSet = session.execute(CassConversationRepo.bind(selectPostboxUnreadCount, email));

            Map<String, Integer> conversationUnreadCounts = StreamUtils.toStream(resultSet).collect(Collectors.toMap(
                    row -> row.getString("conversation_id"),
                    row -> row.getInt("num_unread"))
            );

            List<AbstractConversationThread> conversationThreads = new ArrayList<>();
            AtomicLong newRepliesCount = new AtomicLong();

            ResultSet result = session.execute(CassConversationRepo.bind(selectPostbox, email));

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

            return new PostBox(email, Optional.of(newRepliesCount.get()), conversationThreads, maxAgeDays);
        }
    }

    private Optional<AbstractConversationThread> toConversationThread(String postboxId, String conversationId, String jsonValue, int numUnreadMessages) {
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

    public long getPostboxesCount(Date fromDate, Date toDate) {
        return streamMessageBoxIdsByHour(fromDate, toDate).parallel().count();
    }

    public long getPostboxesCountByQuery(Date fromDate, Date toDate) {
        Statement bound = CassConversationRepo.bind(countPostboxes, fromDate, toDate);
        ResultSet resultset = session.execute(bound);
        return resultset.one().getLong(1);
    }

    public Stream<String> streamMessageBoxIdsByHour(Date fromDate, Date toDate) {
        Statement bound = CassConversationRepo.bind(selectPostboxModifiedBetweenByDate, fromDate, toDate);
        ResultSet resultset = session.execute(bound);
        return toStream(resultset).map(row -> {
            return row.getString(FIELD_POSTBOX_ID) + "/"
                    + row.getString(FIELD_CONVERSATION_ID) + "/"
                    + row.getString(FIELD_MODIFICATION_DATE);
        });

    }


    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }

}
