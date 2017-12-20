package com.ecg.replyts.core.api.model.conversation.event;

import org.joda.time.DateTime;

import java.util.Objects;

public class ConversationEventIndex implements ConversationEventId {

    private final DateTime creationDateRoundedByHour;
    private final String conversationId;

    public ConversationEventIndex(DateTime creationDateRoundedByHour, String conversationId) {
        this.conversationId = conversationId;
        this.creationDateRoundedByHour = creationDateRoundedByHour;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    public DateTime getCreationDateRoundedByHour() {
        return creationDateRoundedByHour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationEventIndex that = (ConversationEventIndex) o;
        return Objects.equals(creationDateRoundedByHour, that.creationDateRoundedByHour) &&
                Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDateRoundedByHour, conversationId);
    }

    @Override
    public String toString() {
        return "ConversationEventIdx{" +
                "ConversationEventIdx=" + creationDateRoundedByHour +
                ", conversationId='" + conversationId + '\'' +
                '}';
    }
}
