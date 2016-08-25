package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationEvents;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationEventsConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationJsonSerializer;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationEventConflictResolver;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Repository;


import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;

import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

@Repository
public class RiakRepo {

    private static final Logger LOG = LoggerFactory.getLogger(RiakRepo.class);

    private final static Timer GET_BY_ID_RIAK_TIMER = TimingReports.newTimer("difftool.riak-getById");

    private final RiakConversationEventConflictResolver resolver;
    private final ConversationEventsConverter converter;

    private IRiakClient riakClient;

    @Autowired
    public RiakRepo(IRiakClient riakClient) {
        this.riakClient = riakClient;
        this.resolver = new RiakConversationEventConflictResolver();
        this.converter = new ConversationEventsConverter(DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME, new ConversationJsonSerializer());
    }

    public Bucket getBucket(String bucketName) throws RiakRetryFailedException {
        return riakClient.fetchBucket(bucketName).execute();
    }

    public ConversationEvents fetchConversation(String convId, Bucket bucket) throws RiakRetryFailedException {
        try (Timer.Context ignored = GET_BY_ID_RIAK_TIMER.time()) {
            return bucket.fetch(convId, ConversationEvents.class).
                    withConverter(converter).
                    withResolver(resolver).
                    withRetrier(new DefaultRetrier(3)).
                    notFoundOK(false).
                    execute();
        }
    }

    public StreamingOperation<IndexEntry> modifiedBetween(DateTime start, DateTime end, Bucket bucket) {
        try {
            return bucket.fetchIndex(
                    IntIndex.named(DiffToolConfiguration.RIAK_SECONDARY_INDEX_MODIFIED_AT)).
                    from(timestampInMinutes(start)).
                    to(timestampInMinutes(end)).executeStreaming();
        } catch (RiakException e) {
            String errMess = bucket.getName() + ": modified between '" + start + "' and '" + end + "' search failed";
            LOG.error(errMess, e);
            throw new RuntimeException(errMess, e);
        }
    }

    public long getConversationCount(DateTime start, DateTime end, String bucketName) throws RiakRetryFailedException {
        AtomicLong counter = new AtomicLong();
        modifiedBetween(start, end, getBucket(bucketName)).forEach(c -> counter.getAndIncrement());
        return counter.get();
    }

    @PreDestroy
    void close() {
        riakClient.shutdown();
    }
}
