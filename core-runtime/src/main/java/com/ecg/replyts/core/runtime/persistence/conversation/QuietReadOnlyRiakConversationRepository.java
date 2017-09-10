package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class QuietReadOnlyRiakConversationRepository extends RiakConversationRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuietReadOnlyRiakConversationRepository.class);

    public QuietReadOnlyRiakConversationRepository(IRiakClient riakClient, String bucketNamePrefix, boolean allowSiblings, boolean lastWriteWins) {
        super(riakClient, bucketNamePrefix, allowSiblings, lastWriteWins);
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
