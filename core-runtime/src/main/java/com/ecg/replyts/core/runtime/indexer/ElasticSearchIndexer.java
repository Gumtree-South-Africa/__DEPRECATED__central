package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.joda.time.DateTimeZone.UTC;

@Component
public class ElasticSearchIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private static final Timer BATCH_INDEX_TIMER = TimingReports.newTimer("indexer.streaming-batch-conversation-index-timer");

    private final AtomicLong taskCounter = new AtomicLong(0);
    private final AtomicInteger completedBatches = new AtomicInteger(0);
    private final AtomicLong submittedConvCounter = new AtomicLong(0);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private Conversation2Kafka conversation2Kafka;

    @Value("${replyts.indexer.streaming.threadcount:32}")
    private int threadCount;

    @Value("${replyts.indexer.streaming.queue.size:100}")
    private int workQueueSize;

    @Value("${replyts.indexer.streaming.conversationid.batch.size:3000}")
    private int conversationIdBatchSize;

    @Value("${replyts.indexer.streaming.timeout.sec:600}")
    private int taskCompletionTimeoutSec;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    private ExecutorService threadPoolExecutor;
    private final CurrentClock clock = new CurrentClock();

    @PostConstruct
    public void initialize() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", ElasticSearchIndexer.class.getSimpleName());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.threadPoolExecutor = new InstrumentedExecutorService(executor, "indexer", ElasticSearchIndexer.class.getSimpleName());
    }

    @PreDestroy
    public void shutdown() {
        threadPoolExecutor.shutdown();
        try {
            LOG.info("Indexing terminating due to shutdown");
            if (!threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOG.warn("Some of the thread haven't completed during the graceful period, going to interrupt them...");
            }
            threadPoolExecutor.shutdownNow();
            LOG.info("Indexing terminated due to shutdown");
        } catch (InterruptedException e) {
            LOG.warn("Indexing termination failed", e);
            Thread.currentThread().interrupt();
        }
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo) {
        LOG.info("Started indexing between {} and {}", dateFrom, dateTo);

        indexConversations(conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo));

        LOG.info("Indexing completed. Total {} conversations, {} fetched documents, {} tasks completed", submittedConvCounter.get(), conversation2Kafka.fetchedConvCounter.get(), taskCounter.get());

        submittedConvCounter.set(0);
        completedBatches.set(0);
        taskCounter.set(0);
        conversation2Kafka.fetchedConvCounter.set(0);
    }

    public void indexConversations(Stream<String> conversationIds) {
        List<Future> futures = new ArrayList<>();

        Iterators.partition(conversationIds.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
            try (Timer.Context ignore = BATCH_INDEX_TIMER.time()) {
                Set<String> uniqueIds = new HashSet<>(ids);

                submittedConvCounter.addAndGet(uniqueIds.size());
                taskCounter.incrementAndGet();

                LOG.info("Scheduling for indexing batch {} containing {} conversations, {} submitted in total", taskCounter.get(),
                        uniqueIds.size(), submittedConvCounter.get());

                futures.add(threadPoolExecutor.submit(() -> conversation2Kafka.indexChunk(uniqueIds)));
            }
        });

        waitForCompletion(futures, completedBatches, LOG, taskCompletionTimeoutSec);
    }

    public void fullIndex() {
        String startMsg = "Full Indexing started at " + new DateTime().withZone(UTC);
        LOG.debug(startMsg);

        doIndexBetween(startTimeForFullIndex(), DateTime.now());

        String endMsg = "Full Indexing finished at " + new DateTime().withZone(UTC);
        LOG.debug(endMsg);

    }

    public void indexSince(DateTime since) {
        doIndexBetween(since, DateTime.now());
    }

    DateTime startTimeForFullIndex() {
        return new DateTime(clock.now()).minusDays(maxAgeDays);
    }

    static void waitForCompletion(List<? extends Future> tasks, AtomicInteger counter, Logger log, int completionTimeoutSec) {
        tasks.parallelStream().forEach(t -> {
            try {
                counter.incrementAndGet();
                t.get(completionTimeoutSec, TimeUnit.SECONDS);
            } catch (InterruptedException in) {
                log.warn("Interrupted during waiting for completion on ES index task");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("Exception during waiting for completion on ES index task", e);
            } catch (Exception e) {
                log.error("Exception during waiting for completion on ES index task", e);
            }
        });
    }

}