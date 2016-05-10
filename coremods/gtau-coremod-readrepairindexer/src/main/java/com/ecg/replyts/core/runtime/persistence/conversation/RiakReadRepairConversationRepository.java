package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.replay;

/**
 * Created by mdarapour.
 */
public class RiakReadRepairConversationRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(RiakReadRepairConversationRepository.class);

    private static final String DEFAULT_BUCKET_NAME = "conversation";

    private final ReadRepairConversationBucket conversationBucket;

    public RiakReadRepairConversationRepository(IRiakClient riakClient) {
        this(riakClient, DEFAULT_BUCKET_NAME);
    }

    public RiakReadRepairConversationRepository(IRiakClient riakClient, String bucketName) {
        this(new ReadRepairConversationBucket(riakClient, bucketName));
    }

    RiakReadRepairConversationRepository(ReadRepairConversationBucket conversationBucket) {
        this.conversationBucket = conversationBucket;
    }

    public MutableConversation getById(String conversationId) {
        ConversationEvents conversationEvents = conversationBucket.byId(conversationId);
        if (conversationEvents == null) {
            return null;
        }
        return new DefaultMutableConversation(replay(conversationEvents.getEvents()));
    }

    public Integer hasSiblings(String conversationId) {
        return conversationBucket.hasSiblings(conversationId);
    }
}
