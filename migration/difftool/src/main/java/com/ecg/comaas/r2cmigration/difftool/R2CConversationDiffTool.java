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

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;


public class R2CConversationDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(R2CConversationDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("conversation.mismatch");

    final static Counter RIAK_TO_CASS_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("riak-mismatch-counter");
    final static Counter CASS_TO_RIAK_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("cass-mismatch-counter");

    private final static Timer RIAK_TO_CASS_BATCH_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.batch-compare-timer");
    private final static Timer CASS_TO_RIAK_BATCH_COMPARE_TIMER = TimingReports.newTimer("cass-to-riak.batch-compare-timer");

    private final static Timer RIAK_TO_CASS_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.compare-timer");
    private final static Timer CASS_TO_RIAK_COMPARE_TIMER = TimingReports.newTimer("cass-to-riak.compare-timer");


    Counter cassConversationCounter;
    Counter cassEventCounter;
    Counter riakEventCounter;
    Counter riakConversationCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;

    private DateTime endDate;
    private DateTime startDate;
    private int tzShiftInMin;
    private int idBatchSize;
    private int maxEntityAge;

    @Autowired
    RiakConversationRepo riakRepo;

    @Autowired
    CassConversationRepo cassRepo;

    @Autowired
    ExecutorService executor;

    public R2CConversationDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        cassConversationCounter = newCounter("cassConversationCounter");
        cassEventCounter = newCounter("cassEventCounter");
        riakEventCounter = newCounter("riakEventCounter");
        riakConversationCounter = newCounter("riakConversationCounter");
    }

    public void setDateRange(DateTime startDate, DateTime endDate, int tzShiftInMin) {
        this.tzShiftInMin = tzShiftInMin;
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
            LOG.info("Comparing last {} days, starting from {}", maxEntityAge, this.startDate);
        }
    }

    public long getConversationsCountInTimeSlice(boolean riakToCass) throws RiakException {
        if (riakToCass) {
            // Fetch riak counters
            return riakRepo.getConversationCount(startDate, endDate);
        } else {
            // Fetch cass counters
            return getCassandraConversationCount();
        }
    }

    public long getCassandraConversationModByDayCount() {
        return cassRepo.getConversationModByDayCount(startDate, endDate);
    }

    public long getCassandraConversationCount() {
        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Cassandra Conversation count between startDate {} endDate {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);
        return cassRepo.getConversationModCount(tzStart, tzEnd);
    }

    public List<Future> compareRiakToCassandra() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakRepo.getConversationBucket();
        StreamingOperation<IndexEntry> convIdStream = riakRepo.modifiedBetween(startDate, endDate, convBucket);
        Iterators.partition(convIdStream.iterator(), idBatchSize).forEachRemaining(convIdIdx -> {
            results.add(executor.submit(() -> {
                compareRiakToCassAsync(convIdIdx, convBucket);
            }));
        });
        return results;
    }

    public static final Comparator<ConversationEvent> MODIFIED_DATE_EVENTID = Comparator.comparing(
            (ConversationEvent c) -> c.getConversationModifiedAt().getMillis()).reversed()
            .thenComparing(c -> c.getEventId());

    void compareRiakToCassAsync(List<IndexEntry> conversationIdsFromRiak, Bucket convBucket) {
        try (Timer.Context ignored = RIAK_TO_CASS_BATCH_COMPARE_TIMER.time()) {

            for (IndexEntry convIdIdx : conversationIdsFromRiak) {

                try (Timer.Context timer2 = RIAK_TO_CASS_COMPARE_TIMER.time()) {

                    LOG.debug("Next riak convId {}", convIdIdx.getObjectKey());
                    String convId = convIdIdx.getObjectKey();

                    List<ConversationEvent> riakConvEvents = riakRepo.fetchConversation(convId, convBucket).getEvents();
                    List<ConversationEvent> cassConvEvents = cassRepo.getById(convId);

                    riakConversationCounter.inc();
                    riakEventCounter.inc(riakConvEvents.size());

                    if (cassConvEvents.size() != riakConvEvents.size()) {
                        logConvEventDifference(convId, cassConvEvents, riakConvEvents, true);

                    } else {
                        Collections.sort(riakConvEvents, MODIFIED_DATE_EVENTID);
                        Collections.sort(cassConvEvents, MODIFIED_DATE_EVENTID);

                        for (int i = 0; i < cassConvEvents.size(); i++) {
                            ConversationEvent cassConvEvent = cassConvEvents.get(i);
                            ConversationEvent riakConvEvent = riakConvEvents.get(i);
                            if (!cassConvEvent.equals(riakConvEvent)) {
                                LOG.warn("NOT SAME\n cassEvent:{} \n riakEvent:{} \n\n", cassConvEvent, riakConvEvent);
                                MISMATCH_LOG.info("convid: {}, cassEventId: {}, riakEventId: {}", convId, cassConvEvent.getEventId(), riakConvEvent.getEventId());
                                isRiakMatchesCassandra = false;
                                RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.inc();
                            }
                        }
                    }
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    public List<Future> compareCassandraToRiak() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakRepo.getConversationBucket();

        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Streaming Cassandra Conversation events between startDate {} endDate {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);

        // This stream contains duplicated conversationIds, as well as ConversationEvents
        Stream<Map.Entry<String, List<ConversationEvent>>> conversationStream = cassRepo.findEventsModifiedBetween(tzStart, tzEnd);

        Iterators.partition(conversationStream.iterator(), idBatchSize).forEachRemaining(cassConvEvents -> {
            results.add(executor.submit(() -> {
                compareCassToRiakAsync(cassConvEvents, convBucket);
            }));
        });
        return results;
    }

    void compareCassToRiakAsync(List<Map.Entry<String, List<ConversationEvent>>> allCassConvEvents, Bucket convBucket) {
        try (Timer.Context ignored = CASS_TO_RIAK_BATCH_COMPARE_TIMER.time()) {

            Set<String> convIds = ConcurrentHashMap.newKeySet();
            for (Map.Entry<String, List<ConversationEvent>> cassConvEventsEntry : allCassConvEvents) {

                try (Timer.Context timer2 = CASS_TO_RIAK_COMPARE_TIMER.time()) {

                    String convId = cassConvEventsEntry.getKey();
                    if (StringUtils.isBlank(convId)) {
                        // Hmm empty conversation Id, skip
                        continue;
                    }
                    if (convIds.contains(convId)) {
                        LOG.debug("Skipping next cassandra convId {} as it was already verified", convId);
                        continue;
                    }
                    convIds.add(convId);

                    List<ConversationEvent> cassConvEvents = cassConvEventsEntry.getValue();

                    cassConversationCounter.inc();
                    cassEventCounter.inc(cassConvEvents.size());

                    LOG.debug("Next cassandra convId = " + convId);

                    ConversationEvents riakConvEvent = riakRepo.fetchConversation(convId, convBucket);
                    List<ConversationEvent> riakEvents = riakConvEvent != null ? riakConvEvent.getEvents() : null;

                    if (riakEvents == null && cassConvEventsEntry == null) {
                        continue;
                    }
                    if (cassConvEvents.size() != riakEvents.size()) {
                        logConvEventDifference(convId, cassConvEvents, riakEvents, false);
                    } else {
                        Collections.sort(riakEvents, MODIFIED_DATE_EVENTID);
                        Collections.sort(cassConvEvents, MODIFIED_DATE_EVENTID);

                        for (int i = 0; i < cassConvEvents.size(); i++) {
                            ConversationEvent cassEvent = cassConvEvents.get(i);
                            ConversationEvent riakEvent = riakEvents.get(i);

                            if (!cassEvent.equals(riakEvent)) {
                                logC2RMismatch(convId, cassEvent, riakEvent);
                            }
                        }
                    }
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    private void logC2RMismatch(String convId, ConversationEvent cassEvent, ConversationEvent riakEvent) {
        String nullVal = "NOT KNOWN";
        String cassEventId = cassEvent == null ? nullVal : cassEvent.getEventId();
        String riakEventId = riakEvent == null ? nullVal : riakEvent.getEventId();
        MISMATCH_LOG.info("convid: {}, cass eventid: {}, riak eventid: {} DO NOT MATCH", convId, cassEventId, riakEventId);
        LOG.info("Cassandra conversation event {}", cassEvent == null ? nullVal : cassEvent);
        LOG.info("Riak conversation event {}", riakEvent == null ? nullVal : riakEvent);
        LOG.info("---------------------------------------------------------------");
        isCassandraMatchesRiak = false;
        CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc();
    }

    public static final Comparator<ConversationEvent> MODIFICATION_DATE = (ConversationEvent c1, ConversationEvent c2) ->
            c1.getConversationModifiedAt().compareTo(c2.getConversationModifiedAt());

    private String conEventListToString(List<ConversationEvent> conversations) {
        StringBuilder objstr = new StringBuilder();
        conversations.stream().sorted(MODIFICATION_DATE.reversed()).forEachOrdered(c -> objstr.append(c + "\n"));
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
            isRiakMatchesCassandra = false;
        } else {
            CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.inc(cassConvEvents.size());
            isCassandraMatchesRiak = false;
        }
    }
}
