package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.BulkIndexer;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class BulkIndexerAction implements IndexerAction {
    private static final Logger LOG = LoggerFactory.getLogger(BulkIndexerAction.class);

    private static final Timer BATCH_INDEX_TIMER = TimingReports.newTimer("indexer.streaming-batch-conversation-index-timer");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private IndexerBulkHandler indexerBulkHandler;

    @Value("${replyts.indexer.streaming.threadcount:32}")
    private int threadCount;

    @Value("${replyts.indexer.streaming.queue.size:100}")
    private int workQueueSize;

    @Value("${replyts.indexer.streaming.conversationid.batch.size:3000}")
    private int conversationIdBatchSize;

    @Value("${replyts.indexer.streaming.timeout.sec:600}")
    private int taskCompletionTimeoutSec;

    private final AtomicLong taskCounter = new AtomicLong(0);

    private final AtomicInteger completedBatches = new AtomicInteger(0);

    private final AtomicLong submittedConvCounter = new AtomicLong(0);

    private ExecutorService threadPoolExecutor;

    @PostConstruct
    public void initialize() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", BulkIndexerAction.class.getSimpleName());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.threadPoolExecutor = new InstrumentedExecutorService(executor, "indexer", BulkIndexerAction.class.getSimpleName());
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

    @Override
    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        LOG.info("Started indexing between {} and {}", dateFrom, dateTo);

        indexConversations(conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo));

        try {
            indexerBulkHandler.flush();
        } catch (Exception e) {
            LOG.error("Indexing failed with exception", e);
        }

        LOG.info("Indexing completed. Total {} conversations, {} fetched documents, {} tasks completed", submittedConvCounter.get(), BulkIndexer.getFetchedDocumentCount(), taskCounter.get());

        indexerBulkHandler.resetCounters();
        submittedConvCounter.set(0);
        completedBatches.set(0);
        taskCounter.set(0);
    }

    public void indexConversations(Stream<String> conversations) {
        List<Future> futures = new ArrayList<>();

        Iterators.partition(conversations.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
            Set<String> uniqueIds = new HashSet<>(ids);

            submittedConvCounter.addAndGet(uniqueIds.size());
            taskCounter.incrementAndGet();

            LOG.info("Scheduling for indexing batch {} containing {} conversations, {} submitted in total", taskCounter.get(), uniqueIds.size(), submittedConvCounter.get());

            futures.add(threadPoolExecutor.submit(() -> indexAsync(uniqueIds)));
        });

        Util.waitForCompletion(futures, completedBatches, LOG, taskCompletionTimeoutSec);
    }

    private void indexAsync(Set<String> uniqueIds) {
        try (Timer.Context ignore = BATCH_INDEX_TIMER.time()) {
            indexerBulkHandler.indexChunk(uniqueIds);
        }
    }
}