package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.basho.riak.client.builders.RiakObjectBuilder.newBuilder;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

/**
 * Maps a {@link com.ecg.replyts.core.api.persistence.ConversationIndexKey} to the corresponding {@link ConversationBucket}.
 */
class ConversationIndexBucket {

    private static final Timer DELETE_CONVERSATION_INDEX = TimingReports.newTimer("cleanupConversationIndex");

    private static final String CONV_CREATED_INDEX = "conv_created";
    private final IRiakClient riakClient;
    private final Bucket bucket;

    private static final ValueSizeConstraint MAXIMUM_VALUE_SIZE_CONSTRAINT = ValueSizeConstraint.maxMb(1);
    private static final ValueSizeConstraint MAXIMUM_KEY_SIZE_CONSTRAINT = ValueSizeConstraint.maxKb(1);

    private static final Logger LOG = LoggerFactory.getLogger(ConversationIndexBucket.class);

    ConversationIndexBucket(IRiakClient riakClient, String bucketName) {
        this.riakClient = riakClient;
        this.bucket = newBucket(bucketName);
    }

    private Bucket newBucket(String bucketName) {
        try {
            return riakClient.fetchBucket(bucketName).withRetrier(new DefaultRetrier(3)).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationIndexBucket: could not fetch bucket " + bucketName, e);
        }
    }

    Optional<String> findConversationId(ConversationIndexKey key) {
        try {
            IRiakObject responseObject = bucket
                    .fetch(key.serialize())
                    .r(1) // this could lead to wrong results in an high load pattern but it's ok for this feature
                          // see: http://basho.com/riaks-config-behaviors-part-4/
                    .execute();

            if (responseObject == null) {
                return Optional.absent();
            }

            return Optional.of(responseObject.getValueAsString());
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationIndexBucket: could not find conversation id for key #" + key, e);
        }
    }

    void persist(ConversationCreatedEvent event) {
        ConversationIndexKey key = new ConversationIndexKey(event.getBuyerId(), event.getSellerId(), event.getAdId());
        try {
            byte[] payload = event.getConversationId().getBytes();
            if (MAXIMUM_VALUE_SIZE_CONSTRAINT.isTooBig(payload.length)) {
                throw new IllegalArgumentException("Rejecting to save conversation index key for conversation id " + event.getConversationId() + ". it's too big (" + payload.length + " bytes!)");
            }

            byte[] keyBytes = key.serialize().getBytes();
            if (MAXIMUM_KEY_SIZE_CONSTRAINT.isTooBig(keyBytes.length)) {
                throw new IllegalArgumentException("Rejecting to save conversation index key for conversation id " + event.getConversationId() + ". key is too big (" + keyBytes.length + " bytes!)");
            }

            bucket.store(
                    newBuilder(bucket.getName(), key.serialize())
                            .withContentType("text/plain")
                            .withValue(event.getConversationId())
                            .addIndex(CONV_CREATED_INDEX, timestampInMinutes(event.getCreatedAt()))
                            .build())
                    .w(Quora.QUORUM)
                    .dw(Quora.ONE)
                    .execute();

        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationIndexBucket: could not create conversation index for conversation id #" + event.getConversationId(), e);
        }
    }

    void delete(String key) {
        try (Timer.Context ignored = DELETE_CONVERSATION_INDEX.time()){
            bucket.delete(key).w(1).r(1).rw(1).dw(0).execute();
        } catch (RiakException e) {
            throw new RuntimeException("ConversationIndexBucket: could not delete conversation index " + key, e);
        }
    }

}
