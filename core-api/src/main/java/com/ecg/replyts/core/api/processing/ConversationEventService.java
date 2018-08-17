package com.ecg.replyts.core.api.processing;

import com.ecg.comaas.events.Conversation.Participant;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Set;

public interface ConversationEventService {

    void sendConversationCreatedEvent(String tenant, String adId, String conversationId, Map<String, String> metadata, Set<Participant> participants, DateTime createAt);

    void sendMessageAddedEvent(String tenant, String conversationId, String senderUserId, String messageId, String message, Map<String, String> metaData);

    void sendConversationDeletedEvent(String tenant, String conversationId, Participant participant);
}
