package com.ecg.replyts.core.api.model.conversation.event;

import org.joda.time.DateTime;

import java.util.Objects;
import java.util.UUID;

public class ConversationEventIdx {

    private final DateTime creationDateRoundedByHour;
    private final String conversationId;
    private final UUID eventId;

    public ConversationEventIdx(DateTime creationDateRoundedByHour, String conversationId, UUID eventId) {
        this.conversationId = conversationId;
        this.eventId = eventId;
        this.creationDateRoundedByHour = creationDateRoundedByHour;
    }

    public String getConversationId() {
        return conversationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public DateTime getCreationDateRoundedByHour() {
        return creationDateRoundedByHour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationEventIdx that = (ConversationEventIdx) o;
        return Objects.equals(creationDateRoundedByHour, that.creationDateRoundedByHour) &&
                Objects.equals(conversationId, that.conversationId) &&
                Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDateRoundedByHour, conversationId, eventId);
    }

    @Override
    public String toString() {
        return "ConversationEventIdx{" +
                "ConversationEventIdx=" + creationDateRoundedByHour +
                ", conversationId='" + conversationId + '\'' +
                ", eventId=" + eventId +
                '}';
    }
}
