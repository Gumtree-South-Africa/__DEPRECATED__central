package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;

import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class StreamingIndexerAction implements IndexerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingIndexerAction.class);
    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    private static final Timer BATCH_INDEX_TIMER = TimingReports.newTimer("indexer.streaming-batch-conversation-index-timer");
    private static final Timer QUEUE_DRAIN = TimingReports.newTimer("indexer.streaming-queueDraining");

    private static final Histogram ELASTIC_INDEX_RATE = TimingReports.newHistogram("indexer.streaming-elastic-index-rate-micros-per-doc");

    private final ConversationRepository conversationRepository;
    private final ExecutorService threadPoolExecutor;
    private final IndexerChunkHandler indexerChunkHandler;
    private final int conversationIdBatchSize;
    private final ConcurrentLinkedQueue<ListenableActionFuture<BulkResponse>> indexingTasks;

    private final int maxWaitingTasks;

    private int minFreeMemoryBytes = (int) (Runtime.getRuntime().maxMemory() * 0.07); // Keep reserve of 7% memory
    private AtomicLong taskCounter = new AtomicLong(0);
    private AtomicLong submittedConvCounter = new AtomicLong(0);
    private AtomicLong receivedDocCounter = new AtomicLong(0);
    private AtomicLong elasticResponseTimeMs = new AtomicLong(0);
    private AtomicLong elasticResponseTimeTotal = new AtomicLong(0);
    private ThreadPoolExecutor texec;

    public StreamingIndexerAction(ConversationRepository conversationRepository,
                                  IndexerChunkHandler indexerChunkHandler,
                                  int threadCount,
                                  int workQueueSize,
                                  int conversationIdBatchSize,
                                  int maxWaitingTasks) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", StreamingIndexerAction.class.getSimpleName());

        this.conversationRepository = conversationRepository;
        this.texec = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        this.threadPoolExecutor = new InstrumentedExecutorService(texec, "indexer", StreamingIndexerAction.class.getSimpleName());
        this.indexerChunkHandler = indexerChunkHandler;
        this.conversationIdBatchSize = conversationIdBatchSize;
        this.maxWaitingTasks = maxWaitingTasks;
        this.indexingTasks = new ConcurrentLinkedQueue<>();

        TimingReports.newGauge("indexer.streaming-document-indexed", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return receivedDocCounter.get();
            }
        });
        TimingReports.newGauge("indexer.streaming-queue-size", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return indexingTasks.size();
            }
        });

        TimingReports.newGauge("indexer.streaming-elastic-response-time-ms", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return elasticResponseTimeMs.get();
            }
        });
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        LOGGER.info("Started indexing between {} and {}", dateFrom, dateTo);
        Stream<String> conversations = conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo);
        indexConversations(conversations);
        LOGGER.info("Finished indexing.");
    }

    public void indexConversations(Stream<String> conversations) {
        Iterators.partition(conversations.iterator(), conversationIdBatchSize).forEachRemaining(ids -> {
            Set<String> unqids = new HashSet(ids);
            submittedConvCounter.addAndGet(unqids.size());
            taskCounter.incrementAndGet();
            LOGGER.info("Scheduling for indexing batch {} containing {} conversations, {} submitted in total", taskCounter.get(), unqids.size(), submittedConvCounter.get());
            threadPoolExecutor.submit(() -> {
                indexAsync(unqids);
            });
            if (indexingTasks.size() > maxWaitingTasks || (Runtime.getRuntime().freeMemory() < minFreeMemoryBytes && !indexingTasks.isEmpty())) {
                drainQueue(indexingTasks, false);
            }
        });

        drainQueue(indexingTasks, true);
        LOGGER.info("Total {} conversations, {} fetched documents, {} indexed documents", submittedConvCounter.get(), SearchIndexer.getFetchedDocumentCount(), receivedDocCounter.get());
        submittedConvCounter.set(0);
        receivedDocCounter.set(0);
        taskCounter.set(0);
        elasticResponseTimeTotal.set(0);
    }

    private void indexAsync(Set<String> unqids) {
        try (Timer.Context ignored = BATCH_INDEX_TIMER.time()) {
            List<ListenableActionFuture<BulkResponse>> docs = indexerChunkHandler.indexChunkAsync(unqids);
            indexingTasks.addAll(docs);
        }
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
                    LOGGER.error("Rejected execution ex while indexing: {}", rejected.getMessage(), rejected);
                } catch (Exception ex) {
                    LOGGER.error("Exception while indexing: {}", ex.getMessage(), ex);
                }

                if (brr != null) {
                    receivedDocCounter.addAndGet(brr.getItems().length);
                    elasticResponseTimeMs.set(brr.getTookInMillis());
                    elasticResponseTimeTotal.addAndGet(brr.getTookInMillis());
                    int rate = brr.getItems().length > 0 ? (int) (brr.getTookInMillis() * 1000 / (brr.getItems().length)) : 0;
                    LOGGER.info("Indexed {} documents in {}ms, indexed {} documents in total, {} microsec/per documents ", brr.getItems().length,
                            brr.getTookInMillis(), receivedDocCounter.get(), rate);
                    ELASTIC_INDEX_RATE.update(rate);
                }
            }

            LOGGER.info("Draining completed for {} indexing tasks in {}ms, last conversation id is {}", taskSize, sw.elapsed(TimeUnit.MILLISECONDS),
                    (brr != null ? brr.getItems()[brr.getItems().length - 1].getId() : 0));

            LOGGER.info("Elastic average indexing time is {} microsec per document", receivedDocCounter.get() > 0 && elasticResponseTimeTotal.get() > 0
                    ? (long) (elasticResponseTimeTotal.get() * 1000 / receivedDocCounter.get()) : "0");
        }
    }

}