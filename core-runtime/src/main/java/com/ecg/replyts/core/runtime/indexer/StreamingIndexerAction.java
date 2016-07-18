package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;

import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Service
public class StreamingIndexerAction implements IndexerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingIndexerAction.class);

    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    private static final Timer TIMER = TimingReports.newTimer("indexer.streaming-indexConversations");
    private static final Timer QUEUE_DRAIN = TimingReports.newTimer("indexer.streaming-queueDraining");

    private static final Histogram ELASTIC_INDEX_RATE = TimingReports.newHistogram("indexer.streaming-elastic-index-rate");


    private final ConversationRepository conversationRepository;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final IndexerChunkHandler indexerChunkHandler;
    private final int conversationIdBatchSize;
    private final ConcurrentLinkedQueue<ListenableActionFuture<BulkResponse>> indexingTasks;

    private final int maxWaitingTasks;

    private int minFreeMemoryBytes = (int) (Runtime.getRuntime().maxMemory() * 0.07); // Keep reserve of 7% memory
    private volatile long taskCounter = 0;
    private volatile long submittedConvCounter = 0;
    private volatile long receivedDocCounter = 0;
    private volatile long elasticResponseTimeMs = 0;

    public StreamingIndexerAction(ConversationRepository conversationRepository,
                                  IndexerChunkHandler indexerChunkHandler,
                                  int threadCount,
                                  int workQueueSize,
                                  int conversationIdBatchSize,
                                  int maxWaitingTasks) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.conversationRepository = conversationRepository;
        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        this.indexerChunkHandler = indexerChunkHandler;
        this.conversationIdBatchSize = conversationIdBatchSize;
        this.maxWaitingTasks = maxWaitingTasks;
        this.indexingTasks = new ConcurrentLinkedQueue<>();

        TimingReports.newGauge("indexer.streaming-document-indexed", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return receivedDocCounter;
            }
        });
        TimingReports.newGauge("indexer.streaming-queue-size", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return indexingTasks.size();
            }
        });

    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        try (Timer.Context ignored = TIMER.time()) {
            LOGGER.info("Started indexing");
            Stream<String> conversations = conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo);
            indexConversations(conversations);
            LOGGER.info("Finished indexing.");
        }
    }

    public void indexConversations(Stream<String> conversations) {
        Iterators.partition(conversations.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
            Set<String> unqids = new HashSet(ids);
            submittedConvCounter += unqids.size();
            LOGGER.info("Scheduling for indexing batch {} containing {} conversations, {} submitted in total", taskCounter++, unqids.size(), submittedConvCounter);
            threadPoolExecutor.submit(() -> {
                List<ListenableActionFuture<BulkResponse>> docs = indexerChunkHandler.indexChunkAsync(unqids);
                indexingTasks.addAll(docs);
            });
            if (indexingTasks.size() > maxWaitingTasks || (Runtime.getRuntime().freeMemory() < minFreeMemoryBytes && !indexingTasks.isEmpty())) {
                drainQueue(indexingTasks, false);
            }
        });
        drainQueue(indexingTasks, true);
        LOGGER.info("Total {} conversations, {} fetched documents, {} indexed documents", submittedConvCounter, SearchIndexer.getFetchedDocumentCount(), receivedDocCounter);
    }

    private void drainQueue(ConcurrentLinkedQueue<ListenableActionFuture<BulkResponse>> indexingTasks, boolean finishing) {
        try (Timer.Context ignored = QUEUE_DRAIN.time()) {
            Stopwatch sw = Stopwatch.createStarted();
            LOGGER.debug("Draining queue of {} elements", indexingTasks.size());
            BulkResponse brr = null;
            int taskSize = indexingTasks.size();
            ListenableActionFuture<BulkResponse> resp = null;
            while ((resp = indexingTasks.poll()) != null) {
                int minQueueSize = maxWaitingTasks > 10 ? (int) (maxWaitingTasks * 0.1) : 1;
                if (indexingTasks.size() <= minQueueSize && !finishing) break;
                try {
                    brr = resp.actionGet();
                    if (brr.hasFailures()) {
                        LOGGER.error("Indexing had failed with {}", brr.buildFailureMessage());
                        for (BulkItemResponse bulk : brr.getItems()) {
                            Object response = bulk.getResponse();
                            LOGGER.error("Message: {}", bulk.getFailureMessage());
                            if (response instanceof IndexResponse && response != null) {
                                String docId = ((IndexResponse) response).getId();
                                LOGGER.error(docId);
                                FAILED_IDX.info(docId);
                            }
                        }
                    }
                } catch (CancellationException calcelex) {
                    LOGGER.error("Failed to fetch results for indexingTasks {}", indexingTasks.toString(), calcelex);
                } catch (RejectedExecutionException rejected) {
                    LOGGER.error("Rejected execution ex while indexing: ", rejected);
                }
                if (brr != null) {
                    receivedDocCounter += brr.getItems().length;
                    elasticResponseTimeMs += brr.getTookInMillis();
                    int rate = brr.getItems().length > 0 ? (int) (brr.getTookInMillis() / brr.getItems().length) : 0;
                    LOGGER.info("Indexed {} documents in {}ms, indexed {} documents in total, {} ms/per document ", brr.getItems().length,
                            brr.getTookInMillis(), receivedDocCounter, rate);
                    ELASTIC_INDEX_RATE.update(rate);
                }
            }
            LOGGER.info("Draining completed for {} indexing tasks in {}ms, last conversation id is {}", taskSize, sw.elapsed(TimeUnit.MILLISECONDS),
                    (brr != null ? brr.getItems()[brr.getItems().length - 1].getId() : 0));
            LOGGER.info("Elastic average indexing time is {} ms per document", receivedDocCounter > 0 && elasticResponseTimeMs > 0 ? (int) (elasticResponseTimeMs / receivedDocCounter) : "0");
        }
    }

}