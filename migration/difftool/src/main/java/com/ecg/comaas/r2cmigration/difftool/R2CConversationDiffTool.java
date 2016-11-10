package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.repo.CassConversationRepo;
import com.ecg.comaas.r2cmigration.difftool.repo.RiakConversationRepo;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;

import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationEvents;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;

@Service
public class R2CConversationDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(R2CConversationDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("difftool-conversation.mismatch");

    final static Counter RIAK_TO_CASS_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("difftool.riak-mismatch-counter");
    final static Counter CASS_TO_RIAK_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("difftool.cass-mismatch-counter");

    private final static Timer RIAK_TO_CASS_COMPARE_TIMER = TimingReports.newTimer("difftool.riak-to-cass.compare-timer");
    private final static Timer CASS_TO_RIAK_COMPARE_TIMER = TimingReports.newTimer("difftool.cass-to-riak.compare-timer");

    Counter cassConversationCounter;
    Counter cassEventCounter;
    Counter riakEventCounter;
    Counter riakConversationCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;

    private DateTime endDate;
    private DateTime startDate;
    private int idBatchSize;
    private int maxEntityAge;

    @Autowired
    RiakConversationRepo riakRepo;

    @Autowired
    CassConversationRepo cassRepo;

    @Autowired
    ThreadPoolExecutor executor;

    public R2CConversationDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        cassConversationCounter = newCounter("difftool.cassConversationCounter");
        cassEventCounter = newCounter("difftool.cassEventCounter");
        riakEventCounter = newCounter("difftool.riakEventCounter");
        riakConversationCounter = newCounter("difftool.riakConversationCounter");
    }

    public void setDateRange(DateTime startDate, DateTime endDate) {
        if (endDate != null) {
            this.endDate = endDate;
        } else {
            this.endDate = new DateTime(DateTimeZone.UTC);
        }
        if (startDate != null) {
            this.startDate = startDate;
        } else {
            this.startDate = this.endDate.minusDays(maxEntityAge);
        }
        Preconditions.checkArgument(this.endDate.isBeforeNow());
        Preconditions.checkArgument(this.startDate.isBefore(this.endDate));
        if (startDate != null) {
            LOG.info("Compare between {} and {}", this.startDate, this.endDate);
        } else {
            LOG.info("Comparing last {} days, ending on {}", maxEntityAge, this.endDate);
        }
    }

    public long getConversationsCountInTimeSlice(boolean riakToCass) throws RiakException {
        if(riakToCass) {
            // Fetch riak counters
            return riakRepo.getConversationCount(startDate, endDate, DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);
        } else {
            // Fetch cass counters
            return getCassandraConversationCount();
        }
    }

    public long getCassandraConversationModByDayCount() {
        return cassRepo.getConversationModByDayCount(startDate, endDate);
    }

    public long getCassandraConversationCount() {
        return cassRepo.getConversationModCount(startDate, endDate);
    }

    public List<Future> compareRiakToCassandra() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakRepo.getBucket(DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);
        StreamingOperation<IndexEntry> convIdStream = riakRepo.modifiedBetween(startDate, endDate, convBucket);
        Iterators.partition(convIdStream.iterator(), idBatchSize).forEachRemaining(convIdIdx -> {
            results.add(executor.submit(() -> {
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
                            LOG.warn("NOT SAME: {} \n{} \n\n", cassConvEvent, riakConvEvent);
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

    public List<Future> compareCassandraToRiak() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakRepo.getBucket(DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);

        // This stream contains duplicated conversationIds, as well as ConversationEvents
        Stream<Map.Entry<String, List<ConversationEvent>>> conversationStream = cassRepo.findEventsCreatedBetween(startDate, endDate);

        Iterators.partition(conversationStream.iterator(), idBatchSize).forEachRemaining(cassConvEvents -> {
            results.add(executor.submit(() -> {
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

                ConversationEvents riakConvEvent = riakRepo.fetchConversation(convId, convBucket);
                List<ConversationEvent> riakEvents = riakConvEvent != null ? riakConvEvent.getEvents() : null;

                if (riakEvents == null || cassConvEvents == null) {
                    logC2RMismatch(convId, null, riakEvents);
                    continue;
                }
                if (cassConvEvents.getValue().size() != riakEvents.size()) {
                    logConvEventDifference(convId, cassConvEvents.getValue(), riakEvents, false);
                } else {
                    cassConvEvents.getValue().stream().forEach(cassEvent -> {
                        if (!riakEvents.contains(cassEvent)) {
                            logC2RMismatch(convId, cassEvent, riakEvents);
                        }
                    });
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    private void logC2RMismatch(String convId, ConversationEvent cassEvent, List<ConversationEvent> riakEvents) {
        String nullVal = "NOT KNOWN";
        String cassEventId = cassEvent == null ? nullVal : cassEvent.getEventId();
        MISMATCH_LOG.info("convid: {}, eventid: {}", convId, cassEventId);
        LOG.info("Fails to find cassandra conversation event {}", cassEvent == null ? nullVal : cassEvent);
        LOG.info("in riak events {}", riakEvents == null ? nullVal : riakEvents);
        LOG.info("---------------------------------------------------------------");
        isCassandraMatchesRiak = false;
        CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc();
    }

    public static final Comparator<ConversationEvent> MODIFICATION_DATE = (ConversationEvent c1, ConversationEvent c2) ->
            c1.getConversationModifiedAt().compareTo(c2.getConversationModifiedAt());

    private String conEventListToString(List<ConversationEvent> conversations) {
        StringBuilder objstr = new StringBuilder();
        conversations.stream().sorted(MODIFICATION_DATE.reversed()).forEachOrdered(c -> objstr.append(c+"\n"));
        return objstr.toString();
    }

    private void logConvEventDifference(String convId, List<ConversationEvent> cassConvEvents,
                                        List<ConversationEvent> riakEvents, boolean riakToCassandra) {

        LOG.warn("Riak {} and Cassandra {} number of conversationEvents mismatch for conversationId {}: ",
                riakEvents.size(), cassConvEvents.size(), convId);
        MISMATCH_LOG.info("Cassandra Events Size {}, events:\n{} ", cassConvEvents.size(), conEventListToString(cassConvEvents));
        MISMATCH_LOG.info("Riak Events Size {}, events:\n{}\n", riakEvents.size(), conEventListToString(riakEvents));

        if (riakToCassandra) {
            RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.inc(riakEvents.size());
        } else {
            CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc(cassConvEvents.size());
        }
    }
}
