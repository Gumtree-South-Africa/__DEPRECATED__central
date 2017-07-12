package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;


public class R2CConversationDiffTool extends AbstractDiffTool {

    private static final Logger LOG = LoggerFactory.getLogger(R2CConversationDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("conversation.mismatch");

    final static Counter RIAK_TO_CASS_CONV_MISMATCH_COUNTER = TimingReports.newCounter("riak-conversation-mismatch-counter");
    final static Counter CASS_TO_RIAK_CONV_MISMATCH_COUNTER = TimingReports.newCounter("cass-conversation-mismatch-counter");

    final static Counter RIAK_TO_CASS_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("riak-conversationeevent-mismatch-counter");
    final static Counter CASS_TO_RIAK_EVENT_MISMATCH_COUNTER = TimingReports.newCounter("cass-conversationeevent-mismatch-counter");

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

    private int idBatchSize;

    @Autowired
    private RiakConversationRepo riakConversationRepository;

    @Autowired
    private CassConversationRepo cassandraConversationRepository;

    @Autowired
    private ExecutorService executor;

    R2CConversationDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        cassConversationCounter = newCounter("cassConversationCounter");
        cassEventCounter = newCounter("cassEventCounter");
        riakEventCounter = newCounter("riakEventCounter");
        riakConversationCounter = newCounter("riakConversationCounter");
    }

    long getConversationsCountInTimeSlice(boolean riakToCass) throws RiakException {
        if (riakToCass) {
            // Fetch riak counters
            return riakConversationRepository.getConversationCount(startDate, endDate);
        } else {
            // Fetch cass counters
            return getCassandraConversationCount();
        }
    }

    long getCassandraConversationCount() {
        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Cassandra Conversation count between startDate {} endDate {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);
        return cassandraConversationRepository.getConversationModCount(tzStart, tzEnd);
    }

    List<Future> compareRiakToCassandra() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakConversationRepository.getConversationBucket();
        StreamingOperation<IndexEntry> convIdStream = riakConversationRepository.modifiedBetween(startDate, endDate, convBucket);
        Iterators.partition(convIdStream.iterator(), idBatchSize).forEachRemaining(convIdIdx -> results.add(executor.submit(() -> compareRiakToCassAsync(convIdIdx, convBucket))));
        return results;
    }

    void compareRiakToCassandra(String... conversationIds) throws RiakException {
        List<IndexEntry> indexEntries = Arrays.stream(conversationIds)
                .map(IndexEntry::new)
                .collect(Collectors.toList());

        compareRiakToCassAsync(indexEntries, riakConversationRepository.getConversationBucket());
    }

    private static final Comparator<ConversationEvent> MODIFIED_DATE_EVENTID = Comparator.comparing(
            (ConversationEvent c) -> c.getConversationModifiedAt().getMillis()).reversed()
            .thenComparing(ConversationEvent::getEventId);

    private void compareRiakToCassAsync(List<IndexEntry> conversationIdsFromRiak, Bucket convBucket) {
        try (Timer.Context ignored = RIAK_TO_CASS_BATCH_COMPARE_TIMER.time()) {

            for (IndexEntry convIdIdx : conversationIdsFromRiak) {
                String conversationId = convIdIdx.getObjectKey();
                LOG.debug("Next riak conversationId {}", conversationId);

                try (Timer.Context ignore = RIAK_TO_CASS_COMPARE_TIMER.time()) {
                    List<ConversationEvent> riakConvEvents = riakConversationRepository.fetchConversation(conversationId, convBucket).getEvents();
                    List<ConversationEvent> cassConvEvents = cassandraConversationRepository.getById(conversationId);

                    riakConversationCounter.inc();
                    riakEventCounter.inc(riakConvEvents.size());

                    boolean same = true;
                    if (cassConvEvents.size() != riakConvEvents.size()) {
                        logConversationEventDifference(conversationId, cassConvEvents, riakConvEvents, true);
                        same = false;
                    } else {
                        riakConvEvents.sort(MODIFIED_DATE_EVENTID);
                        cassConvEvents.sort(MODIFIED_DATE_EVENTID);

                        for (int i = 0; i < cassConvEvents.size(); i++) {
                            ConversationEvent cassConvEvent = cassConvEvents.get(i);
                            ConversationEvent riakConvEvent = riakConvEvents.get(i);
                            if (!cassConvEvent.equals(riakConvEvent)) {
                                LOG.warn("NOT SAME\n cassEvent:{} \n riakEvent:{} \n\n", cassConvEvent, riakConvEvent);
                                MISMATCH_LOG.info("convid: {}, cassEventId: {}, riakEventId: {}", conversationId, cassConvEvent.getEventId(), riakConvEvent.getEventId());
                                isRiakMatchesCassandra = false;
                                RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.inc();
                                same = false;
                            }
                        }
                    }
                    if (!same) {
                        RIAK_TO_CASS_CONV_MISMATCH_COUNTER.inc();
                    } else {
                        LOG.debug("Conversation with id {} - riak == cassandra", conversationId);
                    }
                } catch (NullPointerException npe) {
                    LOG.warn("Riak to Cassandra comparison failed for id {}", conversationId, npe);
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    void compareCassandraToRiak(String... conversationIds) throws RiakRetryFailedException {
        // ouch, this does a cassandra get in a for loop...
        List<Map.Entry<String, List<ConversationEvent>>> collect = Arrays.stream(conversationIds)
                .collect(Collectors.toMap(Function.identity(), cassandraConversationRepository::getById))
                .entrySet()
                .stream()
                .collect(Collectors.toList());

        compareCassToRiakAsync(collect, riakConversationRepository.getConversationBucket());
    }

    List<Future> compareCassandraToRiak() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBucket = riakConversationRepository.getConversationBucket();

        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Streaming Cassandra Conversation events between startDate {} endDate {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);

        // This stream contains duplicated conversationIds, as well as ConversationEvents
        Stream<Map.Entry<String, List<ConversationEvent>>> conversationStream = cassandraConversationRepository.findEventsModifiedBetween(tzStart, tzEnd);

        Iterators.partition(conversationStream.iterator(), idBatchSize).forEachRemaining(cassConvEvents -> results.add(executor.submit(() -> compareCassToRiakAsync(cassConvEvents, convBucket))));
        return results;
    }

    private void compareCassToRiakAsync(List<Map.Entry<String, List<ConversationEvent>>> allCassConvEvents, Bucket convBucket) {
        try (Timer.Context ignored = CASS_TO_RIAK_BATCH_COMPARE_TIMER.time()) {

            Set<String> convIds = ConcurrentHashMap.newKeySet();
            for (Map.Entry<String, List<ConversationEvent>> cassConvEventsEntry : allCassConvEvents) {

                try (Timer.Context ignore = CASS_TO_RIAK_COMPARE_TIMER.time()) {

                    String conversationId = cassConvEventsEntry.getKey();
                    if (StringUtils.isBlank(conversationId)) {
                        // Hmm empty conversation Id, skip
                        continue;
                    }
                    if (convIds.contains(conversationId)) {
                        LOG.debug("Skipping next cassandra conversationId {} as it was already verified", conversationId);
                        continue;
                    }
                    convIds.add(conversationId);

                    List<ConversationEvent> cassConvEvents = cassConvEventsEntry.getValue();
                    if (cassConvEvents == null) {
                        LOG.info("No conversation events are found for Cassandra conversationId {} ", conversationId);
                        continue;
                    }

                    cassConversationCounter.inc();
                    cassEventCounter.inc(cassConvEvents.size());

                    LOG.debug("Next cassandra conversationId = " + conversationId);

                    ConversationEvents riakConvEvent = riakConversationRepository.fetchConversation(conversationId, convBucket);
                    List<ConversationEvent> riakEvents = riakConvEvent != null ? riakConvEvent.getEvents() : null;

                    if (riakEvents == null) {
                        LOG.info("Some events ({}) were found in Cassandra but none in Riak", cassConvEvents.size());
                        continue;
                    }

                    boolean same = true;
                    if (cassConvEvents.size() != riakEvents.size()) {
                        logConversationEventDifference(conversationId, cassConvEvents, riakEvents, false);
                        same = false;
                    } else {
                        riakEvents.sort(MODIFIED_DATE_EVENTID);
                        cassConvEvents.sort(MODIFIED_DATE_EVENTID);

                        for (int i = 0; i < cassConvEvents.size(); i++) {
                            ConversationEvent cassEvent = cassConvEvents.get(i);
                            ConversationEvent riakEvent = riakEvents.get(i);
                            Preconditions.checkNotNull(cassEvent, "Some Cassandra events are null");
                            Preconditions.checkNotNull(riakEvent, "Some Riak events are null");
                            if (!cassEvent.equals(riakEvent)) {
                                logC2RMismatch(conversationId, cassEvent, riakEvent);
                                same = false;
                            }
                        }
                    }
                    if (!same) {
                        CASS_TO_RIAK_CONV_MISMATCH_COUNTER.inc();
                    } else {
                        LOG.debug("Conversation with id {} - riak == cassandra", conversationId);
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

    private static final Comparator<ConversationEvent> MODIFICATION_DATE = Comparator.comparing(ConversationEvent::getConversationModifiedAt);

    private String conEventListToString(List<ConversationEvent> conversations) {
        StringBuilder objstr = new StringBuilder();
        conversations.stream().filter(Objects::nonNull).sorted(MODIFICATION_DATE.reversed()).forEachOrdered(c -> objstr.append(c).append("\n"));
        return objstr.toString();
    }

    private void logConversationEventDifference(String convId, List<ConversationEvent> cassConvEvents,
                                                List<ConversationEvent> riakEvents, boolean riakToCassandra) {
        LOG.warn("Mismatching number of conversationEvents for conversationId {} - Riak has {} and Cassandra has {} events",
                convId, riakEvents.size(), cassConvEvents.size());
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
