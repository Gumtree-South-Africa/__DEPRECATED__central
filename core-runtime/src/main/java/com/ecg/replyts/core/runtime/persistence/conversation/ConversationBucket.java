package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.Mutation;
import com.basho.riak.client.cap.Quora;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.ecg.replyts.core.runtime.persistence.FetchIndexHelper.fetchResult;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

class ConversationBucket {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationBucket.class);

    private static final Timer DELETE_CONVERSATION_TIMER = TimingReports.newTimer("cleanupConversation");

    private static final ConversationEvents DEFAULT_EMPTY_CONVERSATION_EVENT = new ConversationEvents(null);

    static final String SECONDARY_INDEX_CREATED_AT = "createdAt";
    static final String SECONDARY_INDEX_MODIFIED_AT = "modifiedAt";

    private final Bucket bucket;
    private final RiakConversationEventConflictResolver resolver;
    private final ConversationEventsConverter converter;

    ConversationBucket(IRiakClient riakClient, String bucketName, boolean allowSiblings, boolean lastWriteWins) {
        try {
            this.bucket = riakClient
                    .updateBucket(riakClient.fetchBucket(bucketName).execute())
                    .allowSiblings(allowSiblings)
                    .lastWriteWins(lastWriteWins)
                    .execute();

            converter = new ConversationEventsConverter(bucketName, new ConversationJsonSerializer());
            resolver = new RiakConversationEventConflictResolver();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("Could not create/update conversation bucket", e);
        }
    }

    public ConversationEvents byId(String conversationId) {
        try {
            return bucket.
                    fetch(conversationId, ConversationEvents.class)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .notFoundOK(true)
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load conversation by id #" + conversationId, e);
        }
    }

    public void write(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        try {
            Mutation<ConversationEvents> mutator = new RiakConversationEventMutator(toBeCommittedEvents);

            bucket.store(conversationId, DEFAULT_EMPTY_CONVERSATION_EVENT)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .withMutator(mutator)
                    .returnBody(false)
                    .w(Quora.QUORUM)
                    .dw(Quora.ONE)
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not write #" + conversationId, e);
        }
    }

    List<String> modifiedBetween(DateTime start, DateTime end) {
        long startMin = timestampInMinutes(start);
        long endMin = timestampInMinutes(end);
        LOG.debug("Fetching ConversationBucket#modifiedBetween {} - {}", startMin, endMin);
        try {
            return bucket.fetchIndex(
                    IntIndex.named(SECONDARY_INDEX_MODIFIED_AT)).from(startMin).to(endMin).execute();
        } catch (RiakException e) {
            throw new RuntimeException("ConversationBucket: modified between '" + startMin + "' and '" + endMin + "' search failed", e);
        }
    }

    Stream<String> modifiedBetweenStream(DateTime start, DateTime end) {
        try {
            long startMin = timestampInMinutes(start);
            long endMin = timestampInMinutes(end);
            LOG.debug("Fetching ConversationBucket#modifiedBetween {} - {}", startMin, endMin);

            Spliterator<IndexEntry> indexEntrySpliterator = bucket.fetchIndex(
                    IntIndex.named(ConversationBucket.SECONDARY_INDEX_MODIFIED_AT))
                    .from(startMin)
                    .to(endMin)
                    .executeStreaming()
                    .spliterator();

            return StreamSupport.stream(indexEntrySpliterator, false).map(IndexEntry::getObjectKey);

        } catch (RiakException e) {
            String errMess = bucket.getName() + ": modified between '" + start + "' and '" + end + "' search failed";
            LOG.error(errMess, e);
            throw new RuntimeException(errMess, e);
        }
    }

    List<String> createdBetween(DateTime start, DateTime end) {
        try {
            return bucket.fetchIndex(
                    IntIndex.named(SECONDARY_INDEX_CREATED_AT)).from(timestampInMinutes(start)).to(timestampInMinutes(end)
            ).execute();
        } catch (RiakException e) {
            throw new RuntimeException("ConversationBucket: modified between '" + start + "' and '" + end + "' search failed", e);
        }
    }

    Set<String> modifiedBefore(DateTime before, int maxRows) {
        try {
            List<IndexEntry> indexEntries = fetchResult(bucket.fetchIndex(IntIndex.named(SECONDARY_INDEX_MODIFIED_AT)), before, maxRows);
            return indexEntries.stream()
                    .map(IndexEntry::getObjectKey)
                    .collect(Collectors.toSet());
        } catch (RiakException e) {
            throw new RuntimeException("ConversationBucket: modified before '" + before + "' max rows '" + maxRows + "' search failed", e);
        }
    }

    public void delete(String id) {
        try (Timer.Context ignored = DELETE_CONVERSATION_TIMER.time()) {
            bucket.delete(id).w(1).r(1).rw(1).dw(0).execute();
        } catch (RiakException e) {
            throw new RuntimeException("could not delete conversation #" + id, e);
        }
    }
}
