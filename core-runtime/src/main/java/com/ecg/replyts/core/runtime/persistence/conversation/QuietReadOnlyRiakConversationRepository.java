package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class QuietReadOnlyRiakConversationRepository extends RiakConversationRepository  {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuietReadOnlyRiakConversationRepository.class);

    public QuietReadOnlyRiakConversationRepository(IRiakClient riakClient, boolean allowSiblings, boolean lastWriteWins) {
        this(riakClient, DEFAULT_BUCKET_NAME, DEFAULT_SECRET_BUCKET_NAME, DEFAULT_INDEX_BUCKET_NAME, allowSiblings, lastWriteWins);
    }

    public QuietReadOnlyRiakConversationRepository(IRiakClient riakClient, String bucketNamePrefix, boolean allowSiblings, boolean lastWriteWins) {
        this(riakClient, bucketNamePrefix + DEFAULT_BUCKET_NAME, bucketNamePrefix + DEFAULT_SECRET_BUCKET_NAME, bucketNamePrefix + DEFAULT_INDEX_BUCKET_NAME, allowSiblings, lastWriteWins);
    }

    public QuietReadOnlyRiakConversationRepository(IRiakClient riakClient, String bucketName, String secretBucketName, String indexBucketName, boolean allowSiblings, boolean lastWriteWins) {
        this(new ConversationSecretBucket(riakClient, secretBucketName), new ConversationBucket(riakClient, bucketName, allowSiblings, lastWriteWins), new ConversationIndexBucket(riakClient, indexBucketName));
    }

    QuietReadOnlyRiakConversationRepository(ConversationSecretBucket conversationSecretBucket, ConversationBucket conversationBucket, ConversationIndexBucket conversationIndexBucket) {
        super(conversationSecretBucket, conversationBucket, conversationIndexBucket);
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        LOGGER.debug("called QuietReadOnlyRiakConversationRepository.commit");
    }

    @Override
    public void deleteConversation(Conversation c) {
        LOGGER.debug("called QuietReadOnlyRiakConversationRepository.deleteConversation");
    }
}
