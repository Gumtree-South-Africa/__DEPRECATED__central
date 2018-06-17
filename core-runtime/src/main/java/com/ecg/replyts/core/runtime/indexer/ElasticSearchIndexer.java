package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class ElasticSearchIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private static final Timer INDEX_TIMER = TimingReports.newTimer("indexer.streaming-conversation-index-timer");

    private final AtomicLong submittedConvCounter = new AtomicLong(0);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private Conversation2Kafka conversation2Kafka;

    @Value("${replyts.indexer.streaming.threadcount:32}")
    private int threadCount;

    @Value("${replyts.indexer.streaming.queue.size:5000}")
    private int workQueueSize;

    @Value("${replyts.indexer.streaming.conversationid.buffer.size:10000}")
    private int convIdDedupBufferSize;

    @Value("${replyts.indexer.streaming.timeout.sec:65}")
    private int taskCompletionTimeoutSec;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    private CompletionService completionService;
    private ExecutorService executorService;
    private ThreadPoolExecutor executor;
    private final CurrentClock clock = new CurrentClock();

    @PostConstruct
    public void initialize() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", ElasticSearchIndexer.class.getSimpleName());

        executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        executorService = new InstrumentedExecutorService(executor, "indexer", ElasticSearchIndexer.class.getSimpleName());
        completionService = new ExecutorCompletionService(executorService);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            LOG.info("Indexing terminating due to shutdown");
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                LOG.warn("Some of the thread haven't completed during the graceful period, going to interrupt them...");
            }
            executorService.shutdownNow();
            LOG.info("Indexing terminated due to shutdown");
        } catch (InterruptedException e) {
            LOG.warn("Indexing termination failed", e);
            Thread.currentThread().interrupt();
        }
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo) {
        LOG.info("Started indexing between {} and {}", dateFrom, dateTo);
        cleanExecutor();
        indexConversations(conversationRepository.streamConversationsModifiedBetween(dateFrom, dateTo));

        LOG.info("Indexing completed. Total {} conversations, {} fetched documents", submittedConvCounter.get(), conversation2Kafka.fetchedConvCounter.get());

        submittedConvCounter.set(0);
        conversation2Kafka.fetchedConvCounter.set(0);
    }

    private void cleanExecutor() {
        executor.purge();
        executor.getQueue().clear();
    }

    public void indexConversations(Stream<String> conversationIds) {

        // Use this as  a temporary buffer to reduce duplication
        final Set<String> uniqueConvIds = ConcurrentHashMap.newKeySet(convIdDedupBufferSize);
        conversationIds.parallel().forEach(id -> {

            if (uniqueConvIds.add(id)) {

                try (Timer.Context ignore = INDEX_TIMER.time()) {

                    submittedConvCounter.incrementAndGet();
                    completionService.submit(() -> conversation2Kafka.updateElasticSearch(id), id);

                    if (submittedConvCounter.get() % convIdDedupBufferSize == 0) {
                        removeCompleted();
                        uniqueConvIds.clear();
                    }

                }
            } else {
                LOG.debug("Duplicate id {}, skipping", id);
            }
        });
        awaitCompletion(taskCompletionTimeoutSec, TimeUnit.SECONDS);
        LOG.info("Indexing complete, total conversation indexed {}", submittedConvCounter.get());
    }

    private void removeCompleted() {
        awaitCompletion(0, TimeUnit.SECONDS);
    }

    private void awaitCompletion(int taskCompletionTimeoutSec, TimeUnit timeUnit) {
        String indexedCid = "";
        Future<String> future = null;
        do {
            try {
                future = completionService.poll(taskCompletionTimeoutSec, timeUnit);
                // Ignore the result
                indexedCid = future.get();
            } catch (InterruptedException in) {
                LOG.warn("Interrupted during waiting for completion on ES index task, indexing conversation {}", indexedCid);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOG.error("Exception during waiting for completion on ES index task, indexing conversation {}", indexedCid, e);
            }
        } while (future != null);
    }

    public void fullIndex() {
        doIndexBetween(startTimeForFullIndex(), DateTime.now());
    }

    public void indexSince(DateTime since) {
        doIndexBetween(since, DateTime.now());
    }

    DateTime startTimeForFullIndex() {
        return new DateTime(clock.now()).minusDays(maxAgeDays);
    }

}