package com.ecg.messagecenter.core.persistence.simple;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.Token;
import com.datastax.driver.core.TokenRange;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.CassandraRepository;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.StatementsBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class CassandraSimplePostBoxRepository implements SimplePostBoxRepository, CassandraRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraSimplePostBoxRepository.class);

    private static final String LEGACY_CONVERSATION_THREAD_CLASS = "com.ecg.messagecenter.persistence.ConversationThread";

    private static final String FIELD_POSTBOX_ID = "postbox_id";
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    private static final String FIELD_MODIFICATION_DATE = "modification_date";
    private static final String FIELD_ROUNDED_MODIFICATION_DATE = "rounded_modification_date";
    private static final String FIELD_JSON = "json_value";
    private static final String FIELD_NUM_UNREAD = "num_unread";

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer byIdTimer = TimingReports.newTimer("cassandra.postBoxRepo-byId");
    private final Timer unreadCountsByIdTimer = TimingReports.newTimer("cassandra.postBoxRepo-unreadCountsById");
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

    @Value("${persistence.cassandra.conversation.class}")
    private String conversationThreadClass;

    @Value("${replyts.cleanup.conversation.streaming.queue.size:500}")
    private int workQueueSize;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int threadCount;

    @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
    private int batchSize;

    @Value("${comaas.cleanup.postbox.fetchSize:5000}")
    private int fetchSize;

    @Value("${comaas.cleanup.postbox.retryLowerConsistency:false}")
    private boolean retryCleanupLowerConsistency;

    private Class<? extends AbstractConversationThread> conversationClazz;

    @PostConstruct
    public void initializePreparedStatements() {
        this.preparedStatements = StatementsBase.prepare(Statements.class, session);

        try {
            this.conversationClazz = (Class<? extends AbstractConversationThread>) Class.forName(conversationThreadClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ConversationThread class was not found. It would end up with a failing marshalling: " + conversationThreadClass);
        }
    }

    CassandraSimplePostBoxRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
    }

    @Override
    public PostBox byId(PostBoxId id) {
        try (Timer.Context ignored = byIdTimer.time()) {
            Map<String, Integer> unreadCounts = gatherUnreadCounts(id);
            List<AbstractConversationThread> conversationThreads = new ArrayList<>();

            long newRepliesCount = 0;

            ResultSet resultSet = session.execute(Statements.SELECT_POSTBOX.bind(this, id.asString()));

            for (Row row : resultSet) {
                String conversationId = row.getString(FIELD_CONVERSATION_ID);
                String jsonValue = row.getString(FIELD_JSON);

                int unreadCount = unreadCounts.getOrDefault(conversationId, 0);

                Optional<AbstractConversationThread> conversationThread = toConversationThread(id, conversationId, jsonValue, unreadCount);

                if (conversationThread.isPresent()) {
                    conversationThreads.add(conversationThread.get());
                    newRepliesCount += unreadCount;
                }
            }
            LOG.trace("Found {} threads ({} unread) for PostBox with email {} in Cassandra", conversationThreads.size(), newRepliesCount, id.asString());

            return new PostBox(id.asString(), Optional.of(newRepliesCount), conversationThreads);
        }
    }

    @Override
    public PostBox byIdWithoutConversationThreads(PostBoxId id) {
        try (Timer.Context ignored = unreadCountsByIdTimer.time()) {
            Map<String, Integer> unreadCounts = gatherUnreadCounts(id);
            long unreadCount = unreadCounts.values().stream().mapToLong(Integer::longValue).sum();
            return new PostBox(id.asString(), Optional.of(unreadCount), new ArrayList<>());
        }
    }

    private Map<String, Integer> gatherUnreadCounts(PostBoxId id) {
        ResultSet results = session.execute(Statements.SELECT_POSTBOX_UNREAD_COUNTS_CONVERSATION_IDS.bind(this, id.asString()));

        return StreamSupport.stream(results.spliterator(), false)
                .collect(Collectors.toMap(row -> row.getString(FIELD_CONVERSATION_ID), row -> row.getInt(FIELD_NUM_UNREAD)));
    }

    private Optional<AbstractConversationThread> toConversationThread(PostBoxId id, String conversationId, String jsonValue, int numUnreadMessages) {
        try {
            // AbstractConversationThread is parameterized so store the effective class in the data

            String jsonContent = jsonValue.substring(jsonValue.indexOf("@@") + 2);

            AbstractConversationThread conversationThread = ObjectMapperConfigurer.getObjectMapper().readValue(jsonContent, conversationClazz);

            conversationThread.setContainsUnreadMessages(numUnreadMessages > 0);

            return Optional.of(conversationThread);
        } catch (IOException e) {
            LOG.error("Could not deserialize post box {} conversation {} json: {}", id.asString(), conversationId, jsonValue, e);

            return Optional.empty();
        }
    }

    private Optional<String> toJson(AbstractConversationThread conversationThread) {
        try {
            // AbstractConversationThread is parameterized so store the effective class in the data

            String jsonContent = ObjectMapperConfigurer.getObjectMapper().writeValueAsString(conversationThread);

            return Optional.of(LEGACY_CONVERSATION_THREAD_CLASS + "@@" + jsonContent);
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
        try (Timer.Context ignored = writeTimer.time()) {
            BatchStatement batch = new BatchStatement();

            for (AbstractConversationThread conversation : conversations) {
                String conversationId = conversation.getConversationId();
                ((PostBox<AbstractConversationThread>) postBox).cloneConversationMarkAsRead(conversationId).ifPresent(readConversation -> {
                    Optional<String> jsonValue = toJson(readConversation);

                    if (jsonValue.isPresent()) {
                        DateTime timestamp = readConversation.getModifiedAt();
                        DateTime roundedToHour = timestamp.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC);
                        batch.add(Statements.UPDATE_CONVERSATION_THREAD.bind(this, jsonValue.get(), postBox.getId().asString(), conversationId));
                        batch.add(Statements.UPDATE_CONVERSATION_THREAD_UNREAD_COUNT.bind(this, 0, postBox.getId().asString(), conversationId));
                        batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_LATEST.bind(this, postBox.getId().asString(), conversationId, timestamp.toDate()));
                        batch.add(Statements.INSERT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this, postBox.getId().asString(), conversationId, timestamp.toDate(), roundedToHour.toDate()));
                    }
                });
            }

            session.execute(batch);
        }

        // Sorting conversation, required as their status was changed (e.g. unread->read)
        postBox.sortConversations();
    }

    /**
     * Used only for a migration, delete candidate.
     */
    void writeThread(PostBoxId id, AbstractConversationThread conversationThread) {
        try (Timer.Context ignored = writeThreadTimer.time()) {
            BatchStatement batch = new BatchStatement();
            int numUnreadMessages = unreadCountInConversation(id, conversationThread.getConversationId());
            writeConversationThreadStatements(batch, id, conversationThread, numUnreadMessages);
            batch.setConsistencyLevel(writeConsistency).setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);

            session.execute(batch);
        }
    }

    @Override
    public boolean cleanup(DateTime time) {
        Date roundedToHour = time.hourOfDay().roundFloorCopy().toDateTime(DateTimeZone.UTC).toDate();
        String roundedToHourStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(roundedToHour);

        LOG.info("Cleanup: Deleting conversations for the date {} and rounded hour {}", time, roundedToHour);

        try (Timer.Context ignored = streamConversationThreadModificationsByHourTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE
                    .bind(this, roundedToHour)
                    .setRetryPolicy(retryCleanupLowerConsistency ? new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE) : FallthroughRetryPolicy.INSTANCE)
                    .setFetchSize(fetchSize);
            ResultSet resultSet;
            try {
                resultSet = session.execute(bound);
            } catch (ReadTimeoutException rte) {
                LOG.error("Getting postbox data for '" + roundedToHourStr + "' timed out", rte);
                return false;
            }

            // After the introduction of token range-grouped jobs, the blocking queue is an overkill (since there's
            // no lazyness in fetching the jobs anymore). Leave it for now. To be removed if effectiveness of grouping
            // is proven.
            BlockingQueue<Collection<ConversationThreadModificationDate>> jobs = new ArrayBlockingQueue<>(this.workQueueSize);
            ExecutorService cleanupExecutor = createCleanupExecutor(this.threadCount, jobs);
            try {
                Metadata metadata = session.getCluster().getMetadata();
                RangeMap<Token, TokenRange> tokenRangeMap = calculateRangeMap(metadata.getTokenRanges());
                Collection<Set<ConversationThreadModificationDate>> postBoxModificationByTokenRange = StreamSupport
                        .stream(resultSet.spliterator(), false)
                        .map(this::createConversationThreadModificationDate)
                        .collect(groupingBy(v -> tokenRangeMap.getEntry(calculateToken(metadata, v)), toSet())) // This gives us O(ln(R)*V) when grouping by token ranges
                        .values();

                for (Set<ConversationThreadModificationDate> postboxesInSameRange : postBoxModificationByTokenRange) {
                    if (postboxesInSameRange == null || postboxesInSameRange.isEmpty()) {
                        continue;
                    }
                    for (List<ConversationThreadModificationDate> batch : Iterables.partition(postboxesInSameRange, batchSize)) {
                        jobs.put(batch);
                    }
                }
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting to add more jobs", e);
                cleanupExecutor.shutdown();
                return false;
            }

            cleanupExecutor.shutdown();
            try {
                // Replicates the original behaviour. An acceptable patter to wait forever. See java.util.concurrent javadoc.
                cleanupExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                LOG.info("Cleanup: Finished deleting conversations");
                return true;
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for the cleanup to complete");
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static RangeMap<Token, TokenRange> calculateRangeMap(Collection<TokenRange> tokenRanges) {
        // RangeMap is preferred over RangeSet because it doesn't merge adjacent ranges
        ImmutableRangeMap.Builder<Token, TokenRange> tokenRangeSetBuilder = ImmutableRangeMap.builder();
        for (TokenRange range : tokenRanges) {
            if (range.isEmpty()) {
                continue;
            }
            if (range.isWrappedAround()) {
                tokenRangeSetBuilder.put(Range.greaterThan(range.getStart()), range);
                tokenRangeSetBuilder.put(Range.atMost(range.getEnd()), range);
            } else {
                tokenRangeSetBuilder.put(Range.openClosed(range.getStart(), range.getEnd()), range);
            }
        }
        return tokenRangeSetBuilder.build();
    }

    private static Token calculateToken(Metadata metadata, ConversationThreadModificationDate c) {
        return metadata.newToken(String.valueOf(Hashing.murmur3_128().hashString(c.getPostboxId(), Charsets.UTF_8).asLong()));
    }

    private ExecutorService createCleanupExecutor(int threadCount, BlockingQueue<Collection<ConversationThreadModificationDate>> jobs) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(CassandraSimplePostBoxRepository.class.getSimpleName() + "-%d")
                .setDaemon(true)
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount, threadFactory);
        for (int ignored = 0; ignored < threadCount; ignored++) {
            executor.submit(setTaskFields(() -> {
                while (!executor.isShutdown()) {
                    try {
                        Collection<ConversationThreadModificationDate> job = jobs.poll(100, TimeUnit.MILLISECONDS);
                        if (job != null) {
                            doCleanup(job);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Exception e) {
                        LOG.error("Error while performing a single iteration of cleanups", e);
                    }
                }
            }, CassandraSimplePostBoxRepository.class.getSimpleName() + ".cleanup"));
        }

        return executor;
    }

    private ConversationThreadModificationDate createConversationThreadModificationDate(Row row) {
        return new ConversationThreadModificationDate(
                row.getString(FIELD_POSTBOX_ID), row.getString(FIELD_CONVERSATION_ID),
                row.getDate(FIELD_MODIFICATION_DATE), row.getDate(FIELD_ROUNDED_MODIFICATION_DATE));
    }

    private void doCleanup(Collection<ConversationThreadModificationDate> idxs) {
        LOG.info("Cleanup: Deleting data related to {} conversation thread modification dates", idxs.size());

        for (ConversationThreadModificationDate conversationThreadModificationDate : idxs) {
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
        }
    }

    @Override
    public Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId) {
        try (Timer.Context ignored = threadByIdTimer.time()) {
            Row conversationThread = session.execute(Statements.SELECT_CONVERSATION_THREAD.bind(this, id.asString(), conversationId)).one();

            if (conversationThread == null) {
                return Optional.empty();
            } else {
                int numUnreadMessages = unreadCountInConversation(id, conversationId);
                return toConversationThread(id, conversationId, conversationThread.getString(FIELD_JSON), numUnreadMessages);
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
        return unreadCount != null ? unreadCount.getInt(FIELD_NUM_UNREAD) : 0;
    }

    @Override
    public int unreadCountInConversations(PostBoxId id, List<AbstractConversationThread> conversations) {
        Map<String, Integer> unreads = gatherUnreadCounts(id);

        return conversations.stream()
                .map(AbstractConversationThread::getConversationId)
                .mapToInt(convid -> unreads.getOrDefault(convid, 0))
                .sum();
    }

    private void deleteEntireConversationThread(PostBoxId id, String conversationId) {
        try (Timer.Context ignored = deleteConversationThreadWithModificationIdxTimer.time()) {
            BatchStatement batch = new BatchStatement();

            deleteConversationThreadStatements(batch, id, conversationId);

            session.execute(batch);
        }
    }

    @Override
    public void deleteConversations(PostBox postBox, List<String> convIds) {
        try (Timer.Context ignored = deleteConversationThreadBatchTimer.time()) {
            BatchStatement batch = new BatchStatement();

            for (String conv : convIds) {
                deleteConversationThreadStatements(batch, PostBoxId.fromEmail(postBox.getEmail()), conv);
            }

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

    private void deleteOnlyConversationThreadModification(ConversationThreadModificationDate modification) {
        try (Timer.Context ignored = deleteOldConversationThreadModificationDateTimer.time()) {
            Statement statement = Statements.DELETE_CONVERSATION_THREAD_MODIFICATION_IDX_BY_DATE.bind(this,
                    modification.getRoundedModificationDate(), modification.getModificationDate(),
                    modification.getPostboxId(), modification.getConversationThreadId()
            );

            session.execute(statement);
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
