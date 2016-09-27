package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.*;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static com.ecg.replyts.core.runtime.TimingReports.*;

public class ChunkedConversationMigrationAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedConversationMigrationAction.class);

    // Never interrupt the migration by timing out
    private static final int MAX_PROCESSING_TIME_DAYS = 360;

    private AtomicLong submittedConvCounter = new AtomicLong();
    private AtomicInteger processedTimeSlices = new AtomicInteger();
    private AtomicInteger totalTimeSlices = new AtomicInteger();

    private final Timer TIMER = TimingReports.newTimer("migration.migrated-conversations-timer");

    private final SingleRunGuard singleRunGuard;
    private final MigrationChunkHandler conversationsChunkHandler;
    private final HybridConversationRepository conversationRepository;
    private final int threadCount;
    private final int chunkSizeMinutes;
    private final int conversationMaxAgeDays;
    private Stopwatch watch;

    public ChunkedConversationMigrationAction(
            SingleRunGuard singleRunGuard,
            HybridConversationRepository conversationRepository,
            MigrationChunkHandler migrationChunkHandler,
            int threadCount,
            int chunkSizeMinutes,
            int conversationMaxAgeDays
    ) {
        this.singleRunGuard = singleRunGuard;
        this.conversationRepository = conversationRepository;
        this.conversationsChunkHandler = migrationChunkHandler;
        this.threadCount = threadCount;
        this.chunkSizeMinutes = chunkSizeMinutes;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        newGauge("migration.processed-time-slices-counter", () -> processedTimeSlices.get());
        newGauge("migration.total-time-slices-counter", () -> totalTimeSlices.get());
        newGauge("migration.submitted-conversation-counter", () -> submittedConvCounter.get());
    }

    public long getExpectedCompletionTime(TimeUnit tunit) {
        return tunit.convert(new Double(((100 - getPercentCompleted()) / 100.0) * watch.elapsed(TimeUnit.SECONDS)).intValue(), TimeUnit.SECONDS);
    }

    public int getAvgConversationPerTimeSlice() {
        return (int) submittedConvCounter.get() / processedTimeSlices.get();
    }

    public String getRateConversationsPerSec() {
        if (submittedConvCounter.get() > 0) {
            return String.format("%.2f conversations per second", (submittedConvCounter.get() / (double) watch.elapsed(TimeUnit.SECONDS)));
        }
        return "";
    }

    public long getTotalConversationsMigrated() {
        return submittedConvCounter.get();
    }

    public long getTimeTaken(TimeUnit tunit) {
        return watch.elapsed(tunit);
    }

    public int getPercentCompleted() {
        if (processedTimeSlices.get() != 0) {
            LOG.debug("Processed {}, total {} slices ", processedTimeSlices.get(), totalTimeSlices.get());
            return new Double(processedTimeSlices.get() * 100 / (double) totalTimeSlices.get()).intValue();
        }
        return 0;
    }

    public boolean migrateAllConversations() {
        LOG.info("Full migration (all conversation starting from {}) started at {}", getStartingTime(), new DateTime());
        try {
            return migrateConversationsFromDate(getStartingTime());
        } catch (RuntimeException e) {
            LOG.error("Full migration failed", e);
            throw new RuntimeException(e);
        }
    }

    public boolean migrateChunk(List<String> conversationIds) {
        boolean jobExecuted = singleRunGuard.runExclusivelyOrSkip(IndexingMode.MIGRATION, () -> {
            conversationsChunkHandler.migrateChunk(conversationIds);
        });
        if (!jobExecuted) {
            LOG.warn("Skipped migrateChunk for the list of conversation ids {} as another process was already performing the migration", conversationIds);
        }
        return jobExecuted;
    }

    public boolean migrateConversationsFromDate(LocalDateTime dateFrom) {
        boolean jobExecuted = singleRunGuard.runExclusivelyOrSkip(IndexingMode.MIGRATION, () -> {
            migrateConversationsBetweenDates(dateFrom, new LocalDateTime());
        });
        if (!jobExecuted) {
            LOG.warn("Skipped migrateConversationsFromDate {} as another process was already performing the migration", dateFrom);
        }
        return jobExecuted;
    }

    public boolean migrateConversationsBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        boolean jobExecuted = singleRunGuard.runExclusivelyOrSkip(IndexingMode.MIGRATION, () -> {
            migrateConversationsBetweenDates(dateFrom, dateTo);
        });
        if (!jobExecuted) {
            LOG.warn("Skipped migrateConversationsBetween {} and {} as another process was already performing the migration", dateFrom, dateTo);
        }
        return jobExecuted;
    }

    private LocalDateTime getStartingTime() {
        return new LocalDateTime(new Date()).minusDays(conversationMaxAgeDays);
    }

    private void migrateConversationsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        DateSliceIterator dateSlices = new DateSliceIterator(Range.closed(dateFrom.toDateTime(), dateTo.toDateTime()), chunkSizeMinutes, MINUTES,
                IndexingMode.MIGRATION.indexingDirection());
        totalTimeSlices.set(dateSlices.chunkCount());
        processedTimeSlices.set(0);
        submittedConvCounter.set(0);
        LOG.info("There are {} time slices, {}m each to run through", dateSlices.chunkCount(), chunkSizeMinutes);
        watch = Stopwatch.createStarted();
        BlockingBatchExecutor<Range<DateTime>> executor = new BlockingBatchExecutor<>("Migrating", threadCount, MAX_PROCESSING_TIME_DAYS, DAYS);

        executor.executeAll(dateSlices, (Range<DateTime> slice) -> {
            return () -> {
                try (Timer.Context ignored = TIMER.time()) {
                    processedTimeSlices.incrementAndGet();
                    List<String> conversationIds = conversationRepository.listConversationsModifiedBetween(slice.lowerEndpoint(), slice.upperEndpoint());
                    if (!conversationIds.isEmpty()) {
                        submittedConvCounter.addAndGet(conversationIds.size());
                        LOG.debug("Migrating conversations from {}, to {}", slice.lowerEndpoint(), slice.upperEndpoint());
                        LOG.debug("Migrating conversation ids: {}", conversationIds.toString());
                        LOG.debug("Migrating {} conversation, migrated so far {}", conversationIds.size(), submittedConvCounter.get());
                        conversationsChunkHandler.migrateChunk(conversationIds);
                    }
                }
            };
        }, IndexingMode.MIGRATION.errorHandlingPolicy());
        LOG.info("MigrateConversationsBetween is complete, migrated {} conversations", submittedConvCounter.get());
        watch.stop();
    }
}