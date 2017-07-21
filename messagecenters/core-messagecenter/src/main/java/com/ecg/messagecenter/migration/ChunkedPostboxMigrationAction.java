package com.ecg.messagecenter.migration;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.HybridSimplePostBoxRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.*;
import com.ecg.replyts.core.runtime.migrator.ResultFetcher;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;

public class ChunkedPostboxMigrationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedPostboxMigrationAction.class);
    private static final Logger FAILED_POSTBOX_IDS = LoggerFactory.getLogger("FailedToFetchPostboxes");

    private final AtomicLong submittedBatchCounter = new AtomicLong();
    private final AtomicInteger processedBatchCounter = new AtomicInteger();
    private final AtomicLong totalPostboxCounter = new AtomicLong();
    private final AtomicLong nonemptyPostboxCounter = new AtomicLong();
    private final AtomicLong migratedConversationThreadCounter = new AtomicLong();

    private final Timer BATCH_MIGRATION_TIMER = TimingReports.newTimer("migration.migrated-postbox-batches-timer");
    private final Timer RECORD_MIGRATION_TIMER = TimingReports.newTimer("migration.migrated-postbox-record-timer");


    private final HybridSimplePostBoxRepository postboxRepository;
    private final int conversationMaxAgeDays;

    private Stopwatch watch;

    @Autowired
    private final ThreadPoolExecutor executor;

    @Autowired
    private final HazelcastInstance hazelcast;

    private final ResultFetcher resultFetcher;

    private final int idBatchSize;

    ChunkedPostboxMigrationAction(
            HazelcastInstance hazelcast,
            HybridSimplePostBoxRepository postboxRepository,
            ThreadPoolExecutor executor,
            int idBatchSize,
            int conversationMaxAgeDays,
            int completionTimeoutSec
    ) {
        super();
        this.hazelcast = hazelcast;
        this.postboxRepository = postboxRepository;
        this.idBatchSize = idBatchSize;
        this.executor = executor;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        this.resultFetcher = new ResultFetcher(completionTimeoutSec, processedBatchCounter, LOG);
        newGauge("migration.processed-batch-counter", () -> processedBatchCounter.get());
        newGauge("migration.postboxes-counter", () -> totalPostboxCounter.get());
        newGauge("migration.nonempty-postboxes-counter", () -> nonemptyPostboxCounter.get());
        newGauge("migration.submitted-batch-counter", () -> submittedBatchCounter.get());
    }

    public long getExpectedCompletionTime(TimeUnit tunit) {
        return tunit.convert(new Double(((100 - getPercentCompleted()) / 100.0) * watch.elapsed(TimeUnit.SECONDS)).intValue(), TimeUnit.SECONDS);
    }

    public String getRatePostboxesPerSec() {
        if (submittedBatchCounter.get() > 0) {
            return String.format("%.2f ", (submittedBatchCounter.get() * idBatchSize / (double) watch.elapsed(TimeUnit.SECONDS)));
        }
        return "";
    }

    public long getPostboxBatchesMigrated() {
        return processedBatchCounter.get();
    }

    public long getTotalPostboxes() {
        return totalPostboxCounter.get();
    }

    public long getTimeTaken(TimeUnit tunit) {
        return watch.elapsed(tunit);
    }

    public int getTotalBatches() {
        return new Double(Math.ceil((double) totalPostboxCounter.get() / idBatchSize)).intValue();
    }

    public int getPercentCompleted() {
        if (processedBatchCounter.get() > 0) {
            LOG.debug("Processed {}, of total {} batches ", processedBatchCounter.get(), getTotalBatches());
            return new Double(processedBatchCounter.get() * 100 / (double) submittedBatchCounter.get()).intValue();
        }
        return 0;
    }


    private LocalDateTime getStartingTime() {
        return new LocalDateTime(new Date()).minusDays(conversationMaxAgeDays);
    }

    public boolean migrateAllPostboxes() {
        LOG.info("Full postbox migration started at {}", new DateTime());
        return migratePostboxesFromDate(getStartingTime());
    }

    public boolean migrateChunk(List<String> postboxIds) {
        String msg = String.format(" migrateChunk for the list of postbox ids %s ", postboxIds);
        return execute(() -> fetchPostboxes(postboxIds), msg);
    }

    public boolean migratePostboxesFromDate(LocalDateTime dateFrom) {
        String msg = String.format(" migratePostboxesFromDate %s ", dateFrom);
        return execute(() -> migratePostboxesBetweenDates(dateFrom, new LocalDateTime()), msg);
    }

    public boolean migratePostboxesBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String msg = String.format(" migratePostboxesBetween %s and %s ", dateFrom, dateTo);
        return execute(() -> migratePostboxesBetweenDates(dateFrom, dateTo), msg);
    }

    // This method acquires only one lock which is only released by migratePostboxesBetweenDates method once its done
    private boolean execute(Runnable runnable, String msg) {
        ILock lock = hazelcast.getLock(IndexingMode.MIGRATION.toString());
        try {
            if (lock.tryLock(1L, TimeUnit.SECONDS)) {
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

    private void migratePostboxesBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            List<Future> results = new ArrayList<>();
            watch = Stopwatch.createStarted();
            totalPostboxCounter.set(0);
            processedBatchCounter.set(0);
            submittedBatchCounter.set(0);
            nonemptyPostboxCounter.set(0);
            migratedConversationThreadCounter.set(0);

            Stream<String> postboxStream = postboxRepository.streamPostBoxIds(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            Iterators.partition(postboxStream.iterator(), idBatchSize).forEachRemaining(pboxIdBatch -> {
                results.add(executor.submit(() -> {
                    submittedBatchCounter.incrementAndGet();
                    totalPostboxCounter.addAndGet(pboxIdBatch.size());
                    fetchPostboxes(pboxIdBatch);
                }));
            });

            resultFetcher.waitForCompletion(results);
            watch.stop();
            LOG.info("Migrate postboxes from {} to {} date completed, migrated total {} postboxes, nonempty {} postboxes, " +
                            "conversation threads {}, " +
                            "batches submitted:{}, processed:{}",
                    dateFrom, dateTo, totalPostboxCounter.get(), nonemptyPostboxCounter.get(),
                    migratedConversationThreadCounter.get(),
                    submittedBatchCounter.get(), processedBatchCounter.get());
        } finally {
            try {
                hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
            } catch (Exception e) {
                LOG.error("Failed to release the migration log", e);
            }
        }
    }


    public void fetchPostboxes(List<String> postboxIds) {
        try (Timer.Context ignored = BATCH_MIGRATION_TIMER.time()) {

            int fetchedPostboxCounter = 0;
            for (String postboxId : postboxIds) {

                try (Timer.Context timer = RECORD_MIGRATION_TIMER.time()) {

                    PostBox postbox = postboxRepository.byId(PostBoxId.fromEmail(postboxId));
                    // might be null for very old postbox that have been removed by the cleanup job while the indexer
                    // was running.
                    if (postbox != null) {
                        fetchedPostboxCounter++;
                        int cThreads = postbox.getConversationThreads().size();
                        if (cThreads > 0) {
                            migratedConversationThreadCounter.addAndGet(cThreads);
                            nonemptyPostboxCounter.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    LOG.error(String.format("Migrator could not load postbox %s from repository - skipping it", postboxId), e);
                    FAILED_POSTBOX_IDS.info(postboxId);
                }
            }
            if (fetchedPostboxCounter > 0) {

                LOG.debug("Fetch {} postboxes", fetchedPostboxCounter);
            }
            if (fetchedPostboxCounter != postboxIds.size()) {

                LOG.warn("At least some postbox IDs were not found in the database, {} postboxes expected, but only {} retrieved",
                        postboxIds.size(), fetchedPostboxCounter);
            }
        }
    }

}