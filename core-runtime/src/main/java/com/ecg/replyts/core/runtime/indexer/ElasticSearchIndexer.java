package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class ElasticSearchIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private static final Timer INDEX_TIMER = TimingReports.newTimer("indexer.streaming-conversation-index-timer");
    private final AtomicLong submittedConvCounter = new AtomicLong(0);
    private final CurrentClock clock = new CurrentClock();
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private Conversation2Kafka conversation2Kafka;
    @Value("${replyts.indexer.threadcount:32}")
    private int threadCount;
    @Value("${replyts.indexer.queue.size:5000}")
    private int workQueueSize;
    @Value("${replyts.indexer.conversationid.buffer.size:10000}")
    private int convIdDedupBufferSize;
    @Value("${replyts.indexer.timeout.sec:65}")
    private int taskCompletionTimeoutSec;
    @Value("${replyts.indexer.onfailure.maxRetries:10}")
    private int maxRetriesOnFailure;
    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;
    private CompletionService completionService;
    private ExecutorService executorService;
    private ThreadPoolExecutor executor;

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
            if (!executorService.awaitTermination(taskCompletionTimeoutSec, TimeUnit.SECONDS)) {
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
        this.cleanExecutor();
        List<TimeIntervalPair> pairs = this.getTimeIntervals(dateFrom, dateTo);
        int intervalCount = 0;

        for (TimeIntervalPair timeInterval : pairs) {

            intervalCount++;
            DateTime startInterval = timeInterval.startInterval;
            DateTime endInterval = timeInterval.endInterval;

            for (int i = 1; i <= maxRetriesOnFailure; i++) {
                LOG.info("Starting to index conversations in time interval {} to {}", startInterval, endInterval);
                try {
                    this.indexConversations(conversationRepository.streamConversationsModifiedBetween(startInterval, endInterval));
                    break;
                } catch (Exception ex) {
                    LOG.warn("Failed to index conversation from {} to {}, retrying for {} time", startInterval, endInterval, i, ex);
                }
                if (i == maxRetriesOnFailure) {
                    DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");
                    String msg = String.format("Failed to index time interval %s to %s, giving up after %d retries",
                            formatter.print(startInterval), formatter.print(endInterval), maxRetriesOnFailure);
                    throw new RuntimeException(msg);
                }
            }
            LOG.info("Completed indexing of time interval {} out of {}", intervalCount, pairs.size());
        }

        LOG.info("Indexing completed. Total {} conversations, {} fetched documents, from {} to {}",
                submittedConvCounter.get(), conversation2Kafka.fetchedConvCounter.get(),
                dateFrom, dateTo);

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
                        this.removeCompleted();
                        uniqueConvIds.clear();
                    }

                }
            } else {
                LOG.debug("Duplicate id {}, skipping", id);
            }
        });
        this.awaitCompletion(taskCompletionTimeoutSec, TimeUnit.SECONDS);
    }

    private void removeCompleted() {
        this.awaitCompletion(0, TimeUnit.SECONDS);
    }

    private void awaitCompletion(int taskCompletionTimeoutSec, TimeUnit timeUnit) {
        String indexedCid = "";
        Future<String> future = null;
        do {
            try {
                future = completionService.poll(taskCompletionTimeoutSec, timeUnit);
                if (future != null) {
                    // Ignore the result
                    indexedCid = future.get();
                }
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
        this.doIndexBetween(this.startTimeForFullIndex(), DateTime.now());
    }

    public void indexSince(DateTime since) {
        this.doIndexBetween(since, DateTime.now());
    }

    DateTime startTimeForFullIndex() {
        return new DateTime(clock.now()).minusDays(maxAgeDays);
    }


    List<TimeIntervalPair> getTimeIntervals(DateTime dateFrom, DateTime dateTo) {
        List<TimeIntervalPair> timeIntervalPairs = new ArrayList<>();
        for (DateTime currentDate = dateFrom;
             currentDate.isBefore(dateTo);
             currentDate = currentDate.plusHours(1)) {
            DateTime startInterval = currentDate;
            DateTime endInterval = currentDate.plusHours(1);
            timeIntervalPairs.add(new TimeIntervalPair(startInterval, endInterval));
        }
        return timeIntervalPairs;
    }

    public static class TimeIntervalPair {
        DateTime startInterval;
        DateTime endInterval;

        public TimeIntervalPair(DateTime startInterval, DateTime endInterval) {
            this.startInterval = startInterval;
            this.endInterval = endInterval;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            TimeIntervalPair that = (TimeIntervalPair) o;
            return Objects.equals(startInterval, that.startInterval) &&
                    Objects.equals(endInterval, that.endInterval);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startInterval, endInterval);
        }
    }

}