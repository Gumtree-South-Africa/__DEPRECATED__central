package com.ecg.messagecenter.migration;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.HybridSimplePostBoxRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.*;
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
import static com.ecg.replyts.core.runtime.migrator.Util.waitForCompletion;

public class ChunkedPostboxMigrationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedPostboxMigrationAction.class);
    private static final Logger FAILED_POSTBOX_IDS = LoggerFactory.getLogger("FailedToFetchPostboxes");

    private AtomicLong submittedBatchCounter = new AtomicLong();
    private AtomicInteger processedBatchCounter = new AtomicInteger();
    private AtomicLong totalPostboxCounter = new AtomicLong();

    private final Timer BATCH_MIGRATION_TIMER = TimingReports.newTimer("migration.migrated-postboxes-timer");

    private final HybridSimplePostBoxRepository postboxRepository;
    private final int conversationMaxAgeDays;

    private Stopwatch watch;

    @Autowired
    private final ThreadPoolExecutor executor;

    @Autowired
    private final HazelcastInstance hazelcast;

    private final int idBatchSize;

    ChunkedPostboxMigrationAction(
            HazelcastInstance hazelcast,
            HybridSimplePostBoxRepository postboxRepository,
            ThreadPoolExecutor executor,
            int idBatchSize,
            int conversationMaxAgeDays
    ) {
        super();
        this.hazelcast = hazelcast;
        this.postboxRepository = postboxRepository;
        this.idBatchSize = idBatchSize;
        this.executor = executor;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        newGauge("migration.processed-batch-counter", () -> processedBatchCounter.get());
        newGauge("migration.postboxes-counter", () -> totalPostboxCounter.get());
        newGauge("migration.submitted-batch-counter", () -> submittedBatchCounter.get());
    }

    public long getExpectedCompletionTime(TimeUnit tunit) {
        return tunit.convert(new Double(((100 - getPercentCompleted()) / 100.0) * watch.elapsed(TimeUnit.SECONDS)).intValue(), TimeUnit.SECONDS);
    }

    public String getRatePostboxesPerSec() {
        if (submittedBatchCounter.get() > 0) {
            return String.format("%.2f conversations per second", (submittedBatchCounter.get() * idBatchSize / (double) watch.elapsed(TimeUnit.SECONDS)));
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

            processedBatchCounter.set(0);
            submittedBatchCounter.set(0);

            long totalPostboxes = postboxRepository.getMessagesCount(dateFrom.toDateTime(DateTimeZone.UTC), dateTo.toDateTime(DateTimeZone.UTC));

            Stream<String> postboxStream = postboxRepository.streamPostBoxIds(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            totalPostboxCounter.set(totalPostboxes);

            Iterators.partition(postboxStream.iterator(), idBatchSize).forEachRemaining(pboxIdBatch -> {
                results.add(executor.submit(() -> {
                    submittedBatchCounter.incrementAndGet();
                    fetchPostboxes(pboxIdBatch);
                }));
            });

            waitForCompletion(results, processedBatchCounter, LOG);
            watch.stop();
        } finally {
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
    }


    public void fetchPostboxes(List<String> postboxIds) {
        try (Timer.Context ignored = BATCH_MIGRATION_TIMER.time()) {

            int fetchedPostboxCounter = 0;
            for (String postboxId : postboxIds) {
                try {
                    PostBox postbox = postboxRepository.byId(postboxId);
                    // might be null for very old postbox that have been removed by the cleanup job while the indexer
                    // was running.
                    if (postbox != null) {
                        fetchedPostboxCounter++;
                    }
                } catch (Exception e) {
                    LOG.error(String.format("Migrator could not load postbox {} from repository - skipping it", postboxId), e);
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