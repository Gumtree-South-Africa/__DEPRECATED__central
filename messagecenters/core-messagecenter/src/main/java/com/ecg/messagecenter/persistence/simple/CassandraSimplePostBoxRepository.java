package com.ecg.messagecenter.persistence.simple;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

public class CassandraSimplePostBoxRepository implements SimplePostBoxRepository, CassandraRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraSimplePostBoxRepository.class);

    private static final String FIELD_POSTBOX_ID = "postbox_id";
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_DATE = "modification_date";
    private static final String FIELD_ROUNDED_MODIFICATION_DATE = "rounded_modification_date";

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer byIdTimer = TimingReports.newTimer("cassandra.postBoxRepo-byId");
    private final Timer writeTimer = TimingReports.newTimer("cassandra.postBoxRepo-write");
    private final Timer writeThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo-writeThread");
    private final Timer streamConversationThreadModificationsByHourTimer = TimingReports.newTimer("cassandra.postboxRepo-streamConversationModificationsByHour");
    private final Timer deleteOldConversationThreadModificationDateTimer = TimingReports.newTimer("cassandra.postBoxRepo-deleteOldConversationThreadModificationDate");
    private final Timer deleteConversationThreadWithModificationIdxTimer = TimingReports.newTimer("cassandra.postBoxRepo-deleteConversationThreadWithModificationIdx");
    private final Timer deleteConversationThreadBatchTimer = TimingReports.newTimer("cassandra.postBoxRepo-deleteConversationThreadBatch");
    private final Timer getLastModifiedDateTimer = TimingReports.newTimer("cassandra.postboxRepo-getLastModifiedDate");
    private final Timer threadByIdTimer = TimingReports.newTimer("cassandra.postBoxRepo-threadById");
    private final Timer upsertThreadTimer = TimingReports.newTimer("cassandra.postBoxRepo-upsertThread");

    private Map<StatementsBase, PreparedStatement> preparedStatements;

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

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
    }

    @PostConstruct
    public void initializePreparedStatements() {
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);
    }

    public CassandraSimplePostBoxRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
    }

    @Override
    public PostBox byId(PostBoxId id) {
        try (Timer.Context ignored = byIdTimer.time()) {
            Map<String, Integer> unreadCounts = gatherUnreadCounts(id);
            List<AbstractConversationThread> conversationThreads = new ArrayList<>();

            AtomicLong newRepliesCount = new AtomicLong();

            processThreads(id, row -> {
                String conversationId = row.getString("conversation_id");
                String jsonValue = row.getString("json_value");

                int unreadCount = unreadCounts.getOrDefault(conversationId, 0);

                toConversationThread(id, conversationId, jsonValue, unreadCount)
                        .filter(this::validConversation)
                        .ifPresent(conversation -> {
                            conversationThreads.add(conversation);
                            newRepliesCount.addAndGet(unreadCount);
                        });
            });
            LOG.debug("Found {} threads ({} unread) for PostBox with email {} in Cassandra", conversationThreads.size(), newRepliesCount, id.asString());

            return new PostBox(id.asString(), Optional.of(newRepliesCount.get()), conversationThreads, maxAgeDays);
        }
    }

    private boolean validConversation(AbstractConversationThread conversation) {
        DateTime conversationRetentionTime = DateTime.now().minusDays(maxAgeDays);
        return conversation.getCreatedAt().isAfter(conversationRetentionTime);
    }

    private void processThreads(PostBoxId id, Consumer<Row> action) {
        session.execute(Statements.SELECT_POSTBOX.bind(this, id.asString()))
                .forEach(action);
    }

    private Map<String, Integer> gatherUnreadCounts(PostBoxId id) {
        ResultSet results = session.execute(Statements.SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS.bind(this, id.asString()));

        return StreamSupport.stream(results.spliterator(), false)
                .collect(Collectors.toMap(row -> row.getString("conversation_id"), row -> row.getInt("num_unread")));
    }

    private Optional<AbstractConversationThread> toConversationThread(PostBoxId id, String conversationId, String jsonValue, int numUnreadMessages) {
        try {
            // AbstractConversationThread is parameterized so store the effective class in the data

            String jsonClass = jsonValue.substring(0, jsonValue.indexOf("@@"));
            String jsonContent = jsonValue.substring(jsonValue.indexOf("@@") + 2);

            AbstractConversationThread conversationThread = objectMapper.readValue(jsonContent, (Class<? extends AbstractConversationThread>) Class.forName(jsonClass));

            conversationThread.setContainsUnreadMessages(numUnreadMessages > 0);

            return Optional.of(conversationThread);
        } catch (ClassNotFoundException | IOException e) {
            LOG.error("Could not deserialize post box {} conversation {} json: {}", id.asString(), conversationId, jsonValue, e);

            return Optional.empty();
        }
    }

    private Optional<String> toJson(AbstractConversationThread conversationThread) {
        try {
            // AbstractConversationThread is parameterized so store the effective class in the data

            String jsonClass = conversationThread.getClass().getName();
            String jsonContent = objectMapper.writeValueAsString(conversationThread);

            return Optional.of(jsonClass + "@@" + jsonContent);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialize conversation thread with conversation ID {}", conversationThread.getConversationId(), e);

            return Optional.empty();
        }
    }

    @Override
    public void write(PostBox postBox) {
        PostBoxId postboxId = PostBoxId.fromEmail(postBox.getEmail());

        try (Timer.Context ignored = writeTimer.time()) {
            BatchStatement batch = new BatchStatement();

            Map<String, Integer> conversationToUnreadCount = gatherUnreadCounts(postboxId);
            for (AbstractConversationThread conversationThread : (List<? extends AbstractConversationThread>) postBox.getConversationThreads()) {
                int numUnreadMessages = conversationToUnreadCount.getOrDefault(conversationThread.getConversationId(), 0);
                writeConversationThreadStatements(batch, postboxId, conversationThread, numUnreadMessages);
            }

            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);
        }
    }

    @Override
    public void markConversationsAsRead(PostBox postBox, List<AbstractConversationThread> conversations) {
        PostBoxId postBoxId = PostBoxId.fromEmail(postBox.getEmail());

        try (Timer.Context ignored = writeTimer.time()) {
            BatchStatement batch = new BatchStatement();

            for (AbstractConversationThread conversation: conversations) {
                conversation.setContainsUnreadMessages(false);
                Optional<String> jsonValue = toJson(conversation);

                if (jsonValue.isPresent()) {
                    DateTime timestamp = conversation.getModifiedAt();
                    DateTime roundedToHour = timestamp.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC);
                    batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue.get(), postBoxId.asString(), conversation.getConversationId()));
                    batch.add(Statements.UPDATE_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, 0, postBoxId.asString(), conversation.getConversationId()));
                    batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST.bind(this, postBoxId.asString(), conversation.getConversationId(), timestamp.toDate()));
                    batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this, postBoxId.asString(), conversation.getConversationId(), timestamp.toDate(), roundedToHour.toDate()));
                }
            }

            session.execute(batch);
        }
    }

    /**
     * TODO pbouda: Used only for a migration, delete candidate.
     */
    public void writeThread(PostBoxId id, AbstractConversationThread conversationThread) {
        try (Timer.Context ignored = writeThreadTimer.time()) {
            BatchStatement batch = new BatchStatement();
            int numUnreadMessages = unreadCountInConversation(id, conversationThread.getConversationId());
            writeConversationThreadStatements(batch, id, conversationThread, numUnreadMessages);
            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);
        }
    }

    @Override
    public void cleanup(DateTime time) {
        DateTime roundedToHour = time.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC);

        LOG.info("Cleanup: Deleting conversations for the date {} and rounded hour {}", time, roundedToHour);

        Stream<ConversationThreadModificationDate> conversationThreadModificationsToDelete = streamConversationThreadModificationsByHour(roundedToHour.toDate());

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(conversationThreadModificationsToDelete.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {
                LOG.info("Cleanup: Deleting data related to {} conversation thread modification dates", idxs.size());

                idxs.forEach(conversationThreadModificationDate -> {
                    PostBoxId id = PostBoxId.fromEmail(conversationThreadModificationDate.getPostboxId());
                    String conversationThreadId = conversationThreadModificationDate.getConversationThreadId();

                    DateTime preciseTime = new DateTime(conversationThreadModificationDate.getModificationDate());
                    DateTime lastModified = getLastModifiedDate(id, conversationThreadId);

                    if (lastModified != null && !lastModified.isAfter(preciseTime)) {
                        try {
                            deleteEntireConversationThread(id, conversationThreadId);
                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete Conversation thread {}/{}", id, conversationThreadId, ex);
                        }
                    } else {
                        try {
                            deleteOnlyConversationThreadModification(conversationThreadModificationDate);
                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete " + conversationThreadModificationDate.toString(), ex);
                        }
                    }
                });
            }));
        });

        cleanUpTasks.forEach(task -> {
            try {
                task.get();
            } catch (ExecutionException | RuntimeException e) {
                LOG.error("ConversationThread cleanup task execution failure", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        LOG.info("Cleanup: Finished deleting conversations");
    }

    @Override
    public Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId) {
        try (Timer.Context ignored = threadByIdTimer.time()) {
            Row conversationThread = session.execute(Statements.SELECT_CONVERSATION_THREAD.bind(this, id.asString(), conversationId)).one();

            if (conversationThread == null) {
                return Optional.empty();
            } else {
                int numUnreadMessages = unreadCountInConversation(id, conversationId);
                return toConversationThread(id, conversationId, conversationThread.getString("json_value"), numUnreadMessages);
            }
        }
    }

    @Override
    public Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean incrementUnreadCount) {
        try (Timer.Context ignored = upsertThreadTimer.time()) {
            BatchStatement batch = new BatchStatement();

            if (incrementUnreadCount) {
                int newNumUnreadMessages = unreadCountInConversation(id, conversationThread.getConversationId()) + 1;
                batch.add(Statements.UPDATE_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, newNumUnreadMessages, id.asString(), conversationThread.getConversationId()));
            }

            Optional<String> jsonValue = toJson(conversationThread);

            if (jsonValue.isPresent()) {
                DateTime timestamp = conversationThread.getModifiedAt();
                DateTime roundedToHour = timestamp.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC);
                batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue.get(), id.asString(), conversationThread.getConversationId()));
                batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST.bind(this, id.asString(), conversationThread.getConversationId(), timestamp.toDate()));
                batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this, id.asString(), conversationThread.getConversationId(), timestamp.toDate(), roundedToHour.toDate()));
            }

            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);

            // Return the result as the cumulative number of unread messages in the related PostBox

            return gatherUnreadCounts(id).values().stream()
                    .mapToLong(i -> i)
                    .sum();
        }
    }

    @Override
    public int unreadCountInConversation(PostBoxId id, String conversationId) {
        Row unreadCount = session.execute(Statements.SELECT_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, id.asString(), conversationId)).one();
        return unreadCount != null ? unreadCount.getInt("num_unread") : 0;
    }

    private Stream<ConversationThreadModificationDate> streamConversationThreadModificationsByHour(Date roundedToHour) {
        try (Timer.Context ignored = streamConversationThreadModificationsByHourTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this, roundedToHour);
            ResultSet resultset = session.execute(bound);

            return toStream(resultset).map(row -> new ConversationThreadModificationDate(
                    row.getString(FIELD_POSTBOX_ID), row.getString(FIELD_CONVERSATION_ID),
                    row.getDate(FIELD_MODIFICATION_DATE), row.getDate(FIELD_ROUNDED_MODIFICATION_DATE))
            );
        }
    }

    private void deleteEntireConversationThread(PostBoxId id, String conversationId) {
        try (Timer.Context ignored = deleteConversationThreadWithModificationIdxTimer.time()) {
            BatchStatement batch = new BatchStatement();

            deleteConversationThreadStatements(batch, id, conversationId);

            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);
        }
    }

    @Override
    public void deleteConversations(PostBox postBox, List<String> convIds) {
        try (Timer.Context ignored = deleteConversationThreadBatchTimer.time()) {
            BatchStatement batch = new BatchStatement();
            for(String conv: convIds) {
                deleteConversationThreadStatements(batch, PostBoxId.fromEmail(postBox.getEmail()), conv);
            }
            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);
        }
    }

    private void writeConversationThreadStatements(BatchStatement batch, PostBoxId id, AbstractConversationThread conversationThread, int numUnreadMessages) {
        Optional<String> jsonValue = toJson(conversationThread);

        if (jsonValue.isPresent()) {
            DateTime timestamp = conversationThread.getModifiedAt();
            DateTime roundedToHour = timestamp.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC);

            batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue.get(), id.asString(), conversationThread.getConversationId()));
            batch.add(Statements.UPDATE_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, numUnreadMessages, id.asString(), conversationThread.getConversationId()));
            batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST.bind(this, id.asString(), conversationThread.getConversationId(), timestamp.toDate()));
            batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this, id.asString(), conversationThread.getConversationId(), timestamp.toDate(), roundedToHour.toDate()));
        }
    }

    private void deleteConversationThreadStatements(BatchStatement batch, PostBoxId id, String conversationId) {
        batch.add(Statements.DELETE_CONVERSATION_THREAD.bind(this, id.asString(), conversationId));
        batch.add(Statements.DELETE_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, id.asString(), conversationId));
        batch.add(Statements.DELETE_CONVERSATION_THREAD_MODIFICATION_IDX_ALL.bind(this, id.asString(), conversationId));
    }

    private DateTime getLastModifiedDate(PostBoxId id, String conversationId) {
        try (Timer.Context ignored = getLastModifiedDateTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST.bind(this, id.asString(), conversationId);

            Row result = session.execute(bound).one();

            return result == null ? null : new DateTime(result.getDate(FIELD_MODIFICATION_DATE));
        }
    }

    public void deleteOnlyConversationThreadModification(ConversationThreadModificationDate modification) {
        try (Timer.Context ignored = deleteOldConversationThreadModificationDateTimer.time()) {
            Statement statement = Statements.DELETE_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this,
                    modification.getRoundedModificationDate(), modification.getModificationDate(),
                    modification.getPostboxId(), modification.getConversationThreadId()
            );

            session.execute(statement);
        }
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
        static Statements SELECT_POSTBOX = new Statements("SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id = ?", false);
        static Statements SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS = new Statements("SELECT conversation_id, num_unread FROM mb_unread_counters WHERE postbox_id = ?", false);
        static Statements SELECT_CONVERSATION_THREAD = new Statements("SELECT conversation_id, json_value FROM mb_postbox WHERE postbox_id = ? AND conversation_id = ?", false);
        static Statements UPDATE_CONVERSATION_THREAD = new Statements("UPDATE mb_postbox SET json_value = ? WHERE postbox_id = ? AND conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_THREAD = new Statements("DELETE FROM mb_postbox WHERE postbox_id = ? AND conversation_id = ?", true);
        static Statements SELECT_CONVERSATION_THREAD_UNREAD_COUNT = new Statements("SELECT num_unread FROM mb_unread_counters WHERE postbox_id = ? AND conversation_id = ?", false);
        static Statements UPDATE_CONVERSATION_THREAD_UNREAD_COUNT = new Statements("UPDATE mb_unread_counters SET num_unread = ? WHERE postbox_id = ? and conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_THREAD_UNREAD_COUNT = new Statements("DELETE FROM mb_unread_counters WHERE postbox_id = ? and conversation_id = ?", true);
        static Statements SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE = new Statements("SELECT postbox_id, conversation_id, modification_date, rounded_modification_date FROM mb_postbox_modification_idx_by_date WHERE rounded_modification_date = ?", false);
        static Statements SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST = new Statements("SELECT postbox_id, conversation_id, modification_date FROM mb_postbox_modification_idx WHERE postbox_id = ? AND conversation_id = ? LIMIT 1", false);
        static Statements INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE = new Statements("INSERT INTO mb_postbox_modification_idx_by_date (postbox_id, conversation_id, modification_date, rounded_modification_date) VALUES (?, ?, ?, ?)", false);
        static Statements INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST = new Statements("INSERT INTO mb_postbox_modification_idx (postbox_id, conversation_id, modification_date) VALUES (?, ?, ?)", false);
        static Statements DELETE_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE = new Statements("DELETE FROM mb_postbox_modification_idx_by_date WHERE rounded_modification_date = ? AND modification_date = ? AND postbox_id = ? AND conversation_id = ?", true);
        static Statements DELETE_CONVERSATION_THREAD_MODIFICATION_IDX_ALL = new Statements("DELETE FROM mb_postbox_modification_idx WHERE postbox_id = ? AND conversation_id = ?", true);

        Statements(String cql, boolean modifying) {
            super(cql, modifying);
        }
    }
}
