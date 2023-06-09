package com.ecg.replyts.core.api.processing;

import com.ecg.comaas.events.Conversation.Participant;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ConversationEventService {

    void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Participant> participants, DateTime createAt) throws InterruptedException;

    void sendMessageAddedEvent(String tenant, String conversationId, String senderUserId, String messageId, String message,
                               Map<String, String> metaData, MessageTransport transport, String origin, DateTime receivedAt, List<String> receivingUsers)
            throws InterruptedException;

    void sendConversationDeletedEvent(String tenant, String conversationId, Participant participant) throws InterruptedException;

    void sendConversationReadEvent(String tenant, String conversationId, String userId) throws InterruptedException;

    void sendConversationActivated(String tenant, String conversationId, String userId) throws InterruptedException;

    void sendConversationArchived(String tenant, String conversationId, String userId) throws InterruptedException;
}
