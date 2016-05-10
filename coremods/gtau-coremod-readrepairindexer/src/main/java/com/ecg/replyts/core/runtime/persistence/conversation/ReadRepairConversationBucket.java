package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.cap.Quora;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by mdarapour.
 */
public class ReadRepairConversationBucket extends ConversationBucket {
    private static final Logger LOG = LoggerFactory.getLogger(ReadRepairConversationBucket.class);
    private final Timer getByIdTimer = TimingReports.newTimer("conversationReadRepair-get");
    private final Timer storeTimer = TimingReports.newTimer("conversationReadRepair-store");

    private final Bucket bucket;
    private final ConversationEventsConverter converter;
    private final RiakConversationEventConflictResolver resolver;

    public ReadRepairConversationBucket(IRiakClient riakClient, String bucketName) {
        super(riakClient, bucketName);
        try {
            this.bucket = riakClient.fetchBucket(bucketName).execute();

            ConversationJsonSerializer conversationJsonSerializer = new ConversationJsonSerializer();
            converter = new ConversationEventsConverter(bucketName, conversationJsonSerializer);
            resolver = new RiakConversationEventConflictResolver();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("Could not fetch conversation bucket", e);
        }
    }

    public ConversationEvents byId(String conversationId) {
        try {
            final Timer.Context getContext = getByIdTimer.time();
            ConversationEvents conversationEvents = null;

            try {
                conversationEvents = bucket.fetch(conversationId, ConversationEvents.class).
                        withConverter(converter).
                        withResolver(resolver).
                        notFoundOK(true).
                        execute();
            } finally {
                getContext.stop();
            }

            if(conversationEvents == null || !conversationEvents.needsMerging()) {
                LOG.debug("Conversation {} does not need merging", conversationId);
                return conversationEvents;
            }

            final Timer.Context storeContext = storeTimer.time();
            try {
                return bucket.store(conversationId, conversationEvents)
                        .withConverter(converter)
                        .withResolver(resolver)
                        .returnBody(false)
                        .w(Quora.QUORUM)
                        .dw(Quora.QUORUM)
                        .execute();
            } finally {
                storeContext.stop();
            }
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not merge conversation siblings by id #" + conversationId, e);
        }
    }

    public Integer hasSiblings(String conversationId) {
        final Result result = new Result();
        try {
            bucket.fetch(conversationId, ConversationEvents.class).
                    withConverter(converter).
                    withResolver(new ConflictResolver<ConversationEvents>() {
                        @Override
                        public ConversationEvents resolve(Collection<ConversationEvents> siblings) {
                            if (siblings == null) return null;
                            result.setNumSiblings(siblings.size());
                            return null;
                        }
                    }).
                    notFoundOK(true).
                    execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load conversation by id #" + conversationId, e);
        }
        return result.getNumSiblings();
    }

    class Result {
        private int numSiblings = 0;

        void setNumSiblings(int numSiblings) {
            this.numSiblings = numSiblings;
        }

        int getNumSiblings() {
            return numSiblings;
        }
    }
}
