package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.IndexingMode;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.hazelcast.core.HazelcastInstance;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newGauge;

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

    private final ThreadPoolExecutor executor;

    private final ResultFetcher resultFetcher;

    private final HazelcastInstance hazelcast;

    private final int idBatchSize;

    public ChunkedConversationMigrationAction(
            HazelcastInstance hazelcast,
            HybridConversationRepository conversationRepository,
            int conversationMaxAgeDays,
            ThreadPoolExecutor executor,
            int idBatchSize,
            int completionTimeoutSec
    ) {
        this.hazelcast = hazelcast;
        this.conversationRepository = conversationRepository;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        this.executor = executor;
        this.idBatchSize = idBatchSize;
        this.resultFetcher = new ResultFetcher(completionTimeoutSec, processedBatchCounter, LOG);

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

    private boolean execute(Runnable runnable, String msg) {
        return Util.execute(hazelcast, executor, runnable, msg, LOG);
    }

    public void migrateConversationsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            List<Future> results = new ArrayList<>();
            watch = Stopwatch.createStarted();
            processedBatchCounter.set(0);
            submittedBatchCounter.set(0);
            totalConvIds.set(0);

            Stream<String> convIdStream = conversationRepository.streamConversationsModifiedBetween(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            Iterators.partition(convIdStream.iterator(), idBatchSize).forEachRemaining(convIdIdx ->
                    results.add(executor.submit(() -> {
                        migrateConversations(convIdIdx, conversationRepository::getById);
                        totalConvIds.addAndGet(convIdIdx.size());
                    })));

            resultFetcher.waitForCompletion(results);
            LOG.info("Conversation migration from {} to {} date completed,  {} conversations migrated", dateFrom, dateTo, totalConvIds.get());
            watch.stop();
        } finally {
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
    }

    public MutableConversation migrateConversationsWithDeepComparison(String conversationId) {
        return conversationRepository.getByIdWithDeepComparison(conversationId);
    }


    // This migrates conversations by retrieving them in hybrid mode
    private void migrateConversations(List<String> conversationIds, Function<String, MutableConversation> conversationGetter) {

        try (Timer.Context ignored = BATCH_MIGRATION_TIMER.time()) {

            submittedBatchCounter.incrementAndGet();
            LOG.trace("Migrating conversation ids: {}", conversationIds.toString());
            LOG.debug("Migrating a batch of {} conversations, submitted batches so far: {}/{}", conversationIds.size(), submittedBatchCounter.get(), getTotalBatches());

            List<Conversation> conversations = new ArrayList<>();
            for (String conversationId : conversationIds) {
                try {
                    MutableConversation conversation = conversationGetter.apply(conversationId);
                    // might be null for very old conversation that have been removed by the cleanup job while the indexer
                    // was running, or using the deep comparison getter.
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                } catch (Exception e) {
                    LOG.error("Migrator could not load conversation {} from repository - skipping it", conversationId, e);
                    FAILED_CONVERSATION_IDS.info(conversationId);
                }
            }

            if (conversations.size() > 0) {
                LOG.trace("Fetch {} conversations from {} to {} completed", conversationIds.size(), conversationIds.get(0), conversationIds.get(conversationIds.size() - 1));
            }

            if (conversations.size() != conversationIds.size()) {
                LOG.warn("At least some conversation IDs were not found in the database, {} conversations expected, but only {} retrieved\n" +
                        "Note that this does not need to be an error.", conversationIds.size(), conversations.size());
            }
        }
    }


}