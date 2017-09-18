package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.BulkIndexer;
import com.ecg.replyts.core.runtime.migrator.Util;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;

import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class BulkIndexerAction implements IndexerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkIndexerAction.class);

    private static final Timer BATCH_INDEX_TIMER = TimingReports.newTimer("indexer.streaming-batch-conversation-index-timer");

    private final ConversationRepository conversationRepository;
    private final ExecutorService threadPoolExecutor;
    private final IndexerBulkHandler indexerBulkHandler;
    private final int conversationIdBatchSize;
    private AtomicLong taskCounter = new AtomicLong(0);
    private AtomicInteger completedBatches = new AtomicInteger(0);
    private AtomicLong submittedConvCounter = new AtomicLong(0);
    private final int taskCompletionTimeoutSec;
    public BulkIndexerAction(ConversationRepository conversationRepository,
                             IndexerBulkHandler indexerChunkHandler,
                             int threadCount,
                             int workQueueSize,
                             int conversationIdBatchSize,
                             int taskCompletionTimeoutSec) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", BulkIndexerAction.class.getSimpleName());

        this.conversationRepository = conversationRepository;
        ThreadPoolExecutor texec = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        this.threadPoolExecutor = new InstrumentedExecutorService(texec, "indexer", BulkIndexerAction.class.getSimpleName());
        this.indexerBulkHandler = indexerChunkHandler;
        this.conversationIdBatchSize = conversationIdBatchSize;
        this.taskCompletionTimeoutSec = taskCompletionTimeoutSec;
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        LOGGER.info("Started indexing between {} and {}", dateFrom, dateTo);
        Stream<String> conversations = conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo);
        indexConversations(conversations);

        try {
            indexerBulkHandler.flush();
        }  catch (Exception e) {
            LOGGER.error("Indexing failed with exception", e);
        }
        LOGGER.info("Indexing completed. Total {} conversations, {} fetched documents, {} tasks completed", submittedConvCounter.get(), BulkIndexer.getFetchedDocumentCount(), taskCounter.get());
        indexerBulkHandler.resetCounters();
        submittedConvCounter.set(0);
        completedBatches.set(0);
        taskCounter.set(0);

        LOGGER.info("Finished indexing.");
    }

    public void indexConversations(Stream<String> conversations) {
        List<Future> futures = new ArrayList<>();
        Iterators.partition(conversations.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
            Set<String> unqids = new HashSet<>(ids);
            submittedConvCounter.addAndGet(unqids.size());
            taskCounter.incrementAndGet();
            LOGGER.info("Scheduling for indexing batch {} containing {} conversations, {} submitted in total", taskCounter.get(), unqids.size(), submittedConvCounter.get());
            Future indexingTask = threadPoolExecutor.submit(() -> {
                indexAsync(unqids);
            });
            futures.add(indexingTask);
        });

        Util.waitForCompletion(futures, completedBatches, LOGGER, taskCompletionTimeoutSec);
    }

    private void indexAsync(Set<String> unqids) {
        try (Timer.Context ignored = BATCH_INDEX_TIMER.time()) {
            indexerBulkHandler.indexChunk(unqids);
        }
    }

}