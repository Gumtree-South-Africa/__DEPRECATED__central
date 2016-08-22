package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;

import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterators;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;
import static com.ecg.replyts.core.runtime.TimingReports.*;

@Component
public class RiakCassandraDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(RiakCassandraDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("difftool.mismatch");

    final static Counter RIAK_TO_CASS_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("difftool.riak-mismatch-counter");
    final static Counter CASS_TO_RIAK_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("difftool.cass-mismatch-counter");

    private final static Timer RIAK_TO_CASS_COMPARE_TIMER = TimingReports.newTimer("difftool.riak-to-cass.compare-timer");
    private final static Timer CASS_TO_RIAK_COMPARE_TIMER = TimingReports.newTimer("difftool.cass-to-riak.compare-timer");

    final private ArrayBlockingQueue<Runnable> workQueue;
    final private RejectedExecutionHandler rejectionHandler;
    final ExecutorService threadPoolExecutor;

    Counter cassConversationCounter;
    Counter cassEventCounter;
    Counter riakEventCounter;
    Counter riakConversationCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;

    private int conversationIdBatchSize;

    @Autowired
    RiakRepo riakRepo;

    @Autowired
    CassandraRepo cassRepo;

    private DateTime endDate;
    private DateTime startDate;


    /*
https://gerrit.ecg.so/#/c/40177/2/messagecenters/mp-messagecenter/src/main/java/com/ecg/messagebox/persistence/cassandra/DefaultCassandraPostBoxRepository.java
      this.executorService = new InstrumentedExecutorService(
                new ThreadPoolExecutor(
                        0, getRuntime().availableProcessors() * 2,
                        60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)
                ),

     */

    public RiakCassandraDiffTool(@Value("${replyts.maxConversationAgeDays:180}") int compareNumberOfDays,
                                 @Value("${threadcount:6}") int threadCount,
                                 @Value("${queue.size:100}") int workQueueSize,
                                 @Value("${conversationid.batch.size:1000}") int conversationIdBatchSize) {
        this.conversationIdBatchSize = conversationIdBatchSize;
        this.endDate = new DateTime(DateTimeZone.UTC);
        this.startDate = endDate.minusDays(compareNumberOfDays);
        this.workQueue = new ArrayBlockingQueue<>(workQueueSize);
        this.rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        LOG.info("Comparing last {} days", compareNumberOfDays);

        cassConversationCounter = newCounter("difftool.cassConversationCounter");
        cassEventCounter = newCounter("difftool.cassEventCounter");
        riakEventCounter = newCounter("difftool.riakEventCounter");
        riakConversationCounter = newCounter("difftool.riakConversationCounter");
        /*
        newGauge("difftool.riakConversationCounter", () -> riakConversationCounter.get());
        newGauge("difftool.riakEventCounter", () -> riakEventCounter.get());
        newGauge("difftool.cassEventCounter", () -> cassEventCounter.get());
        newGauge("difftool.cassEventCounter", () -> cassEventCounter.get());
        */
    }

    public long getConversationsToBeMigratedCount() throws RiakException {
        return riakRepo.getConversationCount(startDate, endDate, DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);
    }

    List<Future> compareRiakToCassandra() throws RiakException {
        List<Future> results = new ArrayList<>(conversationIdBatchSize);
        Bucket convBucket = riakRepo.getBucket(DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);
        StreamingOperation<IndexEntry> convIdStream = riakRepo.modifiedBetween(startDate, endDate, convBucket);
        Iterators.partition(convIdStream.iterator(), conversationIdBatchSize).forEachRemaining(convIdIdx -> {
            results.add(threadPoolExecutor.submit(() -> {
                compareRiakToCassAsync(convIdIdx, convBucket);
            }));
        });
        return results;
    }


    void compareRiakToCassAsync(List<IndexEntry> conversationIdsFromRiak, Bucket convBucket) {
        try (Timer.Context ignored = RIAK_TO_CASS_COMPARE_TIMER.time()) {
            for (IndexEntry convIdIdx : conversationIdsFromRiak) {
                LOG.debug("Next riak convId {}", convIdIdx.getObjectKey());
                String convId = convIdIdx.getObjectKey();
                List<ConversationEvent> riakConvEvents = riakRepo.fetchConversation(convId, convBucket).getEvents();
                List<ConversationEvent> cassConvEvents = cassRepo.getById(convId);

                riakConversationCounter.inc();
                riakEventCounter.inc(riakConvEvents.size());

                if (cassConvEvents.size() != riakConvEvents.size()) {
                    logConvEventDifference(convId, cassConvEvents, riakConvEvents, true);

                } else {
                    for (int i = 0; i < cassConvEvents.size(); i++) {
                        ConversationEvent cassConvEvent = cassConvEvents.get(i);
                        ConversationEvent riakConvEvent = riakConvEvents.get(i);
                        if (!cassConvEvent.equals(riakConvEvent)) {
                            LOG.warn("NOT SAME: %s %n%s %n%n ", cassConvEvent, riakConvEvent);
                            MISMATCH_LOG.info("convid: {}, eventid: {}", cassConvEvent, riakConvEvent);
                            isRiakMatchesCassandra = false;
                            RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.inc();
                        }
                    }
                }

                LOG.debug("---------------------------------------------------------------");
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    List<Future> compareCassandraToRiak() throws RiakException {
        List<Future> results = new ArrayList<>(conversationIdBatchSize);
        Bucket convBucket = riakRepo.getBucket(DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);

        // This stream contains duplicated conversationIds, as well as ConversationEvents
        Stream<Map.Entry<String, List<ConversationEvent>>> conversationStream = cassRepo.findEventsCreatedBetween(startDate, endDate);

        Iterators.partition(conversationStream.iterator(), conversationIdBatchSize).forEachRemaining(cassConvEvents -> {
            results.add(threadPoolExecutor.submit(() -> {
                compareCassToRiakAsync(cassConvEvents, convBucket);
            }));
        });
        return results;
    }

    void compareCassToRiakAsync(List<Map.Entry<String, List<ConversationEvent>>> allCassConvEvents, Bucket convBucket) {
        try (Timer.Context ignored = CASS_TO_RIAK_COMPARE_TIMER.time()) {

            HashMap<String, List<ConversationEvent>> checked = new HashMap<>();
            for (Map.Entry<String, List<ConversationEvent>> cassConvEvents : allCassConvEvents) {

                String convId = cassConvEvents.getKey();
                if (checked.containsKey(convId) && checked.get(convId).equals(cassConvEvents.getValue())) {
                    LOG.debug("Skipping next cassandra convId {} as it was already verified", convId);
                    continue;
                }
                checked.put(convId, cassConvEvents.getValue());

                cassConversationCounter.inc();
                cassEventCounter.inc(cassConvEvents.getValue().size());

                LOG.debug("Next cassandra convId = " + convId);
                List<ConversationEvent> riakEvents = riakRepo.fetchConversation(convId, convBucket).getEvents();

                if (cassConvEvents.getValue().size() != riakEvents.size()) {
                    logConvEventDifference(convId, cassConvEvents.getValue(), riakEvents, false);
                } else {
                    cassConvEvents.getValue().stream().forEach(cassEvent -> {
                        if (!riakEvents.contains(cassEvent)) {
                            MISMATCH_LOG.info("convid: {}, eventid: {}", convId, cassEvent.getEventId());
                            LOG.info("Fails to find cassandra conversation event {}", cassEvent);
                            LOG.info("in riak events {}", riakEvents);
                            LOG.info("---------------------------------------------------------------");
                            isCassandraMatchesRiak = false;
                            CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc();
                        }
                    });
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    private void logConvEventDifference(String convId, List<ConversationEvent> cassConvEvents,
                                        List<ConversationEvent> riakEvents, boolean riakToCassandra) {

        LOG.warn("Riak {} and Cassandra {} number of conversationEvents mismatch for conversationId {}: ",
                 riakEvents.size(), cassConvEvents.size(), convId);
        MISMATCH_LOG.info("Cassandra Events Size {}, events: {} ", cassConvEvents.size(), cassConvEvents);
        MISMATCH_LOG.info("Riak Events Size {}, events: {}\n", riakEvents.size(), riakEvents);

        if (riakToCassandra) {
            RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.inc(riakEvents.size());
        } else {
            CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc(cassConvEvents.size());
        }
    }
}
