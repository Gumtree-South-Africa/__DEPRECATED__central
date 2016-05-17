package com.ecg.replyts.core.api.model.conversation.event;

import java.util.UUID;

public class ConversationEventId {

    private final String conversationId;
    private final UUID eventId;

    public ConversationEventId(String conversationId, UUID eventId) {
        this.conversationId = conversationId;
        this.eventId = eventId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
