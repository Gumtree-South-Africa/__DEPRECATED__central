package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.*;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;
import static com.ecg.replyts.core.runtime.migrator.Util.waitForCompletion;

public class ChunkedConversationMigrationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedConversationMigrationAction.class);
    private static final Logger FAILED_CONVERSATION_IDS = LoggerFactory.getLogger("FailedToFetchConversations");

    private AtomicInteger submittedBatchCounter = new AtomicInteger();
    private AtomicInteger processedBatchCounter = new AtomicInteger();
    private AtomicLong totalConvIds = new AtomicLong();

    private final Timer BATCH_MIGRATION_TIMER = TimingReports.newTimer("migration.migrated-conversations-timer");

    private final HybridConversationRepository conversationRepository;

    private final int conversationMaxAgeDays;
    private Stopwatch watch;

    @Autowired
    private final ThreadPoolExecutor executor;

    @Autowired
    private final HazelcastInstance hazelcast;

    private final int idBatchSize;

    public ChunkedConversationMigrationAction(
            HazelcastInstance hazelcast,
            HybridConversationRepository conversationRepository,
            int conversationMaxAgeDays,
            ThreadPoolExecutor executor,
            int idBatchSize
    ) {
        this.hazelcast = hazelcast;
        this.conversationRepository = conversationRepository;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        this.executor = executor;
        this.idBatchSize = idBatchSize;
        newGauge("migration.processed-batch-counter", () -> processedBatchCounter.get());
        newGauge("migration.total-conversation-counter", () -> totalConvIds.get());
        newGauge("migration.submitted-batch-counter", () -> submittedBatchCounter.get());
    }

    public long getExpectedCompletionTime(TimeUnit tunit) {
        return tunit.convert(new Double(((100 - getPercentCompleted()) / 100.0) * watch.elapsed(TimeUnit.SECONDS)).intValue(), TimeUnit.SECONDS);
    }


    public String getRateConversationsPerSec() {
        if (submittedBatchCounter.get() > 0) {
            return String.format("%.2f conversations per second", (submittedBatchCounter.get() * idBatchSize / (double) watch.elapsed(TimeUnit.SECONDS)));
        }
        return "";
    }

    public long getConversationsBatchesMigrated() {
        return processedBatchCounter.get();
    }

    public long getTotalConversations() {
        return totalConvIds.get();
    }

    public long getTimeTaken(TimeUnit tunit) {
        return watch.elapsed(tunit);
    }

    public int getTotalBatches() {
        return new Double(Math.ceil((double) totalConvIds.get() / idBatchSize)).intValue();
    }

    public int getPercentCompleted() {
        if (processedBatchCounter.get() != 0) {
            LOG.debug("Processed {}, of total {} batches ", processedBatchCounter.get(), getTotalBatches());
            return new Double(processedBatchCounter.get() * 100 / (double) submittedBatchCounter.get()).intValue();
        }
        return 0;
    }

    public boolean migrateAllConversations() {
        LOG.info("Full conversation migration started at {}", new LocalDateTime());
        return migrateConversationsFromDate(getStartingTime());
    }

    public boolean migrateChunk(List<String> conversationIds) {
        String msg = String.format(" migrateChunk for the list of conversation ids %s ", conversationIds);
        return execute(() -> fetchConversations(conversationIds), msg);
    }

    public boolean migrateConversationsFromDate(LocalDateTime dateFrom) {
        String msg = String.format(" migrateConversationsFromDate %s ", dateFrom);
        return execute(() -> migrateConversationsBetweenDates(dateFrom, new LocalDateTime()), msg);
    }

    public boolean migrateConversationsBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String msg = String.format(" migrateConversationsBetween %s and %s ", dateFrom, dateTo);
        return execute(() -> migrateConversationsBetweenDates(dateFrom, dateTo), msg);
    }

    private LocalDateTime getStartingTime() {
        return new LocalDateTime(new Date()).minusDays(conversationMaxAgeDays);
    }

    // This method acquires only one lock which is only released by migrateConversationsBetweenDates method once it's done
    private boolean execute(Runnable runnable, String msg) {
        ILock lock = hazelcast.getLock(IndexingMode.MIGRATION.toString());
        try {
            if (lock.tryLock()) {
                executor.execute(runnable);
                LOG.info("Executing {}", msg);
                return true;
            } else {
                LOG.info("Skipped execution {}", msg);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception while waiting on a lock", e);
            lock.unlock();
            executor.getQueue().clear();
            throw new RuntimeException(e);
        }
    }

    public void migrateConversationsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            List<Future> results = new ArrayList<>();
            watch = Stopwatch.createStarted();
            processedBatchCounter.set(0);
            submittedBatchCounter.set(0);

            long convCount = conversationRepository.getConversationCount(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            Stream<String> convIdStream = conversationRepository.streamConversationsModifiedBetween(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            totalConvIds.set(convCount);

            Iterators.partition(convIdStream.iterator(), idBatchSize).forEachRemaining(convIdIdx -> {
                results.add(executor.submit(() -> {
                    fetchConversations(convIdIdx);
                }));
            });

            waitForCompletion(results, processedBatchCounter, LOG);
            LOG.info("Conversation migration from {} to {} date completed,  {} conversations migrated", dateFrom, dateTo, convCount);
            watch.stop();
        } finally {
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
    }


    public void fetchConversations(List<String> conversationIds) {
        try (Timer.Context ignored = BATCH_MIGRATION_TIMER.time()) {

            submittedBatchCounter.incrementAndGet();
            LOG.trace("Migrating conversation ids: {}", conversationIds.toString());
            LOG.debug("Migrating a batch of {} conversations, submitted batches so far: {}", conversationIds.size(), submittedBatchCounter.get());

            List<Conversation> conversations = new ArrayList<>();

            for (String convId : conversationIds) {

                try {
                    MutableConversation conversation = conversationRepository.getById(convId);
                    // might be null for very old conversation that have been removed by the cleanup job while the indexer
                    // was running.
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                } catch (Exception e) {
                    LOG.error(String.format("Migrator could not load conversation {} from repository - skipping it", convId), e);
                    FAILED_CONVERSATION_IDS.info(convId);
                }
            }

            if (conversations.size() > 0) {
                LOG.trace("Fetch {} conversations from {} to {} completed", conversationIds.size(), conversationIds.get(0), conversationIds.get(conversationIds.size() - 1));
            }

            if (conversations.size() != conversationIds.size()) {
                LOG.warn("At least some conversation IDs were not found in the database, {} conversations expected, but only {} retrieved",
                        conversationIds.size(), conversations.size());
            }

        }
    }

}