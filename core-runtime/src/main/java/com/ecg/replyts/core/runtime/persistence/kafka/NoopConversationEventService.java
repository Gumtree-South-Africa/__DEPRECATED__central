package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.comaas.events.Conversation;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "conversation.events.enabled", havingValue = "false", matchIfMissing = true)
public class NoopConversationEventService implements ConversationEventService {
    @Override
    public void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Conversation.Participant> participants, DateTime createAt) {
        // noop
    }

    @Override
    public void sendMessageAddedEvent(String tenant, String conversationId, String senderUserId, String messageId, String message, Map<String, String> metaData) {
        // noop
    }

    @Override
    public void sendConversationDeletedEvent(String tenant, String conversationId, Conversation.Participant participant) {
        // noop
    }
}
