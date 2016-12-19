package com.ecg.comaas.r2cmigration.difftool.repo;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.DiffToolConfiguration;

import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationEvents;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationEventsConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.ConversationJsonSerializer;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationEventConflictResolver;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;


import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;

import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

@Repository
public class RiakConversationRepo {

    private static final Logger LOG = LoggerFactory.getLogger(RiakConversationRepo.class);

    private final static Timer GET_BY_ID_RIAK_TIMER = TimingReports.newTimer("riak-getById");

    private final RiakConversationEventConflictResolver resolver;
    private final ConversationEventsConverter converter;

    private String bucketNamePrefix = "";

    private IRiakClient riakClient;

    @Autowired
    public RiakConversationRepo(IRiakClient riakClient, @Value("${persistence.riak.bucket.name.prefix:}") String bucketNamePrefix) {
        this.riakClient = riakClient;
        this.resolver = new RiakConversationEventConflictResolver();
        if (StringUtils.isNotBlank(bucketNamePrefix)) {
            LOG.info("Using riak bucket prefix: {}", bucketNamePrefix.trim());
            this.bucketNamePrefix = bucketNamePrefix.trim();
        } else {
            LOG.info("No riak bucket prefix configured");
        }
        this.converter = new ConversationEventsConverter(this.bucketNamePrefix + DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME, new ConversationJsonSerializer());
    }

    private Bucket getBucket(String bucketName) throws RiakRetryFailedException {
        LOG.debug("Fetching riak bucket {}", bucketName);
        return riakClient.fetchBucket(bucketName).execute();
    }

    public Bucket getConversationBucket() throws RiakRetryFailedException {
        return getBucket(bucketNamePrefix + DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME);
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
            long startMin = timestampInMinutes(start);
            long endMin = timestampInMinutes(end);
            LOG.debug("Fetching RiakConversationRepo#modifiedBetween {} - {}", startMin, endMin);
            return bucket.fetchIndex(
                    IntIndex.named(DiffToolConfiguration.RIAK_SECONDARY_INDEX_MODIFIED_AT)).
                    from(startMin).
                    to(endMin).executeStreaming();
        } catch (RiakException e) {
            String errMess = bucket.getName() + ": modified between '" + start + "' and '" + end + "' search failed";
            LOG.error(errMess, e);
            throw new RuntimeException(errMess, e);
        }
    }

    public long getConversationCount(DateTime start, DateTime end) throws RiakRetryFailedException {
        AtomicLong counter = new AtomicLong();
        modifiedBetween(start, end, getBucket(bucketNamePrefix + DiffToolConfiguration.RIAK_CONVERSATION_BUCKET_NAME)).forEach(c -> counter.getAndIncrement());
        return counter.get();
    }

    @PreDestroy
    void close() {
        riakClient.shutdown();
    }

}
