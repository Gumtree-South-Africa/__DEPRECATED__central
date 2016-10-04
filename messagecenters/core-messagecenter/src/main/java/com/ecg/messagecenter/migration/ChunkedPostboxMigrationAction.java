package com.ecg.messagecenter.migration;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.HybridSimplePostBoxRepository;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.*;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static com.ecg.replyts.core.runtime.TimingReports.*;

public class ChunkedPostboxMigrationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedPostboxMigrationAction.class);

    // Never interrupt the migration by timing out
    private static final int MAX_PROCESSING_TIME_DAYS = 360;

    private AtomicLong submittedPostboxCounter = new AtomicLong();
    private AtomicInteger processedTimeSlices = new AtomicInteger();
    private AtomicInteger totalTimeSlices = new AtomicInteger();

    private final Timer TIMER = TimingReports.newTimer("migration.migrated-postboxs-timer");

    private final PostboxMigrationChunkHandler postboxChunkHandler;
    private final HybridSimplePostBoxRepository postboxRepository;
    private final int threadCount;
    private final int chunkSizeMinutes;
    private final int conversationMaxAgeDays;
    private Stopwatch watch;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("PostboxMigrationController-%s").build());

    @Autowired
    private final HazelcastInstance hazelcast;

    ChunkedPostboxMigrationAction(
            HazelcastInstance hazelcast,
            HybridSimplePostBoxRepository postboxRepository,
            PostboxMigrationChunkHandler migrationChunkHandler,
            int threadCount,
            int chunkSizeMinutes,
            int conversationMaxAgeDays
    ) {
        super();
        this.hazelcast = hazelcast;
        this.postboxRepository = postboxRepository;
        this.postboxChunkHandler = migrationChunkHandler;
        this.threadCount = threadCount;
        this.chunkSizeMinutes = chunkSizeMinutes;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        newGauge("migration.processed-time-slices-counter", () -> processedTimeSlices.get());
        newGauge("migration.total-time-slices-counter", () -> totalTimeSlices.get());
        newGauge("migration.submitted-postbox-counter", () -> submittedPostboxCounter.get());
    }

    public long getExpectedCompletionTime(TimeUnit tunit) {
        return tunit.convert(new Double(((100 - getPercentCompleted()) / 100.0) * watch.elapsed(TimeUnit.SECONDS)).intValue(), TimeUnit.SECONDS);
    }

    public int getAvgPostboxesPerTimeSlice() {
        return (int) submittedPostboxCounter.get() / processedTimeSlices.get();
    }

    public String getRatePostboxesPerSec() {
        if (submittedPostboxCounter.get() > 0) {
            return String.format("%.2f postboxes per second", (submittedPostboxCounter.get() / (double) watch.elapsed(TimeUnit.SECONDS)));
        }
        return "";
    }

    public long getTotalPostboxesMigrated() {
        return submittedPostboxCounter.get();
    }

    public long getTimeTaken(TimeUnit tunit) {
        return watch.elapsed(tunit);
    }

    private LocalDateTime getStartingTime() {
        return new LocalDateTime(new Date()).minusDays(conversationMaxAgeDays);
    }

    public int getPercentCompleted() {
        if (processedTimeSlices.get() != 0) {
            LOG.debug("Processed {}, total {} slices ", processedTimeSlices.get(), totalTimeSlices.get());
            return new Double(processedTimeSlices.get() * 100 / (double) totalTimeSlices.get()).intValue();
        }
        return 0;
    }

    public boolean migrateAllPostboxes() {
        LOG.info("Full postbox migration started at {}", new DateTime());
        return migratePostboxesFromDate(getStartingTime());
    }

    public boolean migrateChunk(List<String> postboxIds) {
        String msg = String.format(" migrateChunk for the list of postbox ids %s ", postboxIds);
        return execute(() -> postboxChunkHandler.migrateChunk(postboxIds), msg);
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
                executorService.execute(runnable);
                LOG.info("Executing {}", msg);
                return true;
            } else {
                LOG.info("Skipped execution {}", msg);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception while waiting on a lock", e);
            lock.unlock();
            throw new RuntimeException(e);
        }
    }

    private void migratePostboxesBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            DateSliceIterator dateSlices = new DateSliceIterator(Range.closed(dateFrom.toDateTime(DateTimeZone.UTC), dateTo.toDateTime(DateTimeZone.UTC)),
                    chunkSizeMinutes, MINUTES, IndexingMode.MIGRATION.indexingDirection(), DateTimeZone.UTC);

            totalTimeSlices.set(dateSlices.chunkCount());
            processedTimeSlices.set(0);
            submittedPostboxCounter.set(0);
            LOG.info("There are {} time slices, {}m each to run through", dateSlices.chunkCount(), chunkSizeMinutes);
            watch = Stopwatch.createStarted();
            BlockingBatchExecutor<Range<DateTime>> executor = new BlockingBatchExecutor<>("Migrating", threadCount, MAX_PROCESSING_TIME_DAYS, DAYS);
            executor.executeAll(dateSlices, (Range<DateTime> slice) -> {
                return () -> {
                    try (Timer.Context ignored = TIMER.time()) {
                        processedTimeSlices.incrementAndGet();
                        List<String> postboxIds = postboxRepository.getPostBoxIds(slice.lowerEndpoint(), slice.upperEndpoint());
                        if (!postboxIds.isEmpty()) {
                            submittedPostboxCounter.addAndGet(postboxIds.size());
                            LOG.debug("Migrating postboxes from {}, to {}", slice.lowerEndpoint(), slice.upperEndpoint());
                            LOG.debug("Migrating postbox ids: {}", postboxIds.toString());
                            LOG.debug("Migrating {} postboxes, migrated so far {}", postboxIds.size(), submittedPostboxCounter.get());
                            postboxChunkHandler.migrateChunk(postboxIds);
                        }
                    } catch (RuntimeException e) {
                        LOG.error("Postbox migration fails with", e);
                    }
                };
            }, IndexingMode.MIGRATION.errorHandlingPolicy());
            LOG.info("{} PostBoxes actually migrated vs. {} times it was determined necessary for PostBoxes to be migrated", postboxRepository.migratePostBoxCounter.getCount(),
                    postboxRepository.migratePostBoxNecessaryCounter.getCount());
            LOG.info("MigratePostboxesBetween is complete, migrated {} postboxes", submittedPostboxCounter.get());
            watch.stop();
        } finally {
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
    }

}