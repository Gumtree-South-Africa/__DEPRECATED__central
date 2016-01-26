package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.basho.riak.client.builders.RiakObjectBuilder.newBuilder;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

/**
 * Stores Buyer/Seller conversation secrets and maps them to the original plain mail address. May actually fail to perfectly
 * clean up data (one might want to use map/reduce to fix that.
 */
class ConversationSecretBucket {

    private static final Timer DELETE_CONVERSATION_SECRET = TimingReports.newTimer("cleanupSecret");

    private static final String CONV_CREATED_INDEX = "conv_created";
    private final IRiakClient riakClient;
    private final Bucket bucket;

    private final ConversationSecretPayloadEditor editor = new ConversationSecretPayloadEditor();

    private static final ValueSizeConstraint MAXIMUM_SIZE_CONSTRAINT = ValueSizeConstraint.maxMb(1);

    private static final Logger LOG = LoggerFactory.getLogger(ConversationSecretBucket.class);

    ConversationSecretBucket(IRiakClient riakClient, String bucketName) {
        this.riakClient = riakClient;
        this.bucket = newBucket(bucketName);
    }

    private Bucket newBucket(String bucketName) {
        try {
            return riakClient.fetchBucket(bucketName).withRetrier(new DefaultRetrier(3)).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationSecretBucket: could not fetch bucket " + bucketName, e);
        }
    }

    NewConversationCommand findConversationId(String secret) {
        try {
            IRiakObject responseObject = bucket
                    .fetch(secret)
                    .r(1)
                    .notFoundOK(false)  // wait for the first non missing object
                    .execute();

            if (responseObject == null) {
                return null;
            }
            return editor.fromJson(responseObject.getValueAsString());
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationSecretBucket: could not find conversation for secret #" + secret, e);
        }
    }

    void persist(ConversationCreatedEvent event) {
        try {
            byte[] payload = editor.toJson(event).getBytes();
            if (MAXIMUM_SIZE_CONSTRAINT.isTooBig(payload.length)) {
                throw new IllegalArgumentException("Rejecting to save conversation secret for conversation id " + event.getConversationId() + ". it's too big (" + payload.length + " bytes!)");
            }

            bucket.store(
                    newBuilder(bucket.getName(), event.getBuyerSecret())
                            .withValue(payload)
                            .addIndex(CONV_CREATED_INDEX, timestampInMinutes(event.getCreatedAt()))
                            .build())
                    .w(Quora.QUORUM)
                    .dw(Quora.ONE)
                    .execute();

            bucket.store(
                    newBuilder(bucket.getName(), event.getSellerSecret())
                            .withValue(payload)
                            .addIndex(CONV_CREATED_INDEX, timestampInMinutes(event.getCreatedAt()))
                            .build())
                    .w(Quora.QUORUM)
                    .dw(Quora.ONE)
                    .execute();


        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("ConversationSecretBucket: could not create secrets for conversation id #" + event.getConversationId(), e);
        }
    }


    void delete(String secret) {
        try (Timer.Context ignored = DELETE_CONVERSATION_SECRET.time()){
            bucket.delete(secret).w(1).r(1).rw(1).dw(0).execute();
        } catch (RiakException e) {
            throw new RuntimeException("ConversationSecretBucket: could not delete secret " + secret, e);
        }
    }

}
