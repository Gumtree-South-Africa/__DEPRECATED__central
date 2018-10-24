package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.comaas.events.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "conversation.events.enabled", havingValue = "false")
public class NoopConversationEventService implements ConversationEventService {
    @Override
    public void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Conversation.Participant> participants, DateTime createAt) {
        // noop
    }

    @Override
    public void sendMessageAddedEvent(String tenant, String conversationId, Optional<String> senderUserId, String messageId, String message, Map<String, String> metaData, MessageTransport transport, String origin) {
        // noop
    }

    @Override
    public void sendConversationDeletedEvent(String tenant, String conversationId, Conversation.Participant participant) {
        // noop
    }

    @Override
    public void sendConversationReadEvent(String tenant, String conversationId, String userId) throws InterruptedException {
        // noop
    }

    @Override
    public void sendConversationActivated(String tenant, String conversationId, String userId) throws InterruptedException {
        // noop
    }

    @Override
    public void sendConversationArchived(String tenant, String conversationId, String userId) throws InterruptedException {
        // noop
    }
}
