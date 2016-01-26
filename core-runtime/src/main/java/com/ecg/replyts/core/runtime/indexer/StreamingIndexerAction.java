package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class StreamingIndexerAction implements IndexerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingIndexerAction.class);

    private static final Timer TIMER = TimingReports.newTimer("indexer.streaming-indexConversations");

    private final ConversationRepository conversationRepository;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final IndexerChunkHandler indexerChunkHandler;
    private final int conversationIdBatchSize;

    public StreamingIndexerAction(ConversationRepository conversationRepository,
                                  IndexerChunkHandler indexerChunkHandler,
                                  int threadCount,
                                  int workQueueSize,
                                  int conversationIdBatchSize) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.conversationRepository = conversationRepository;
        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        this.indexerChunkHandler = indexerChunkHandler;
        this.conversationIdBatchSize = conversationIdBatchSize;
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        try (Timer.Context ignored = TIMER.time()) {

            LOGGER.info("Started indexing.");

            Stream<String> conversations
                    = conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo);

            List<Future<?>> indexingTasks = new ArrayList<>();

            Iterators.partition(conversations.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
                indexingTasks.add(threadPoolExecutor.submit(() -> {
                    LOGGER.info("Indexing {} conversations", ids.size());
                    indexerChunkHandler.indexChunk(ids);
                }));
            });

            indexingTasks.stream().filter(task -> !task.isDone()).forEach(task -> {
                try {
                    task.get();
                } catch (CancellationException | ExecutionException ignore) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            LOGGER.info("Finished indexing.");
        }
    }
}