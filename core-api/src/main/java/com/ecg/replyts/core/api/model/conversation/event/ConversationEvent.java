package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.util.Assert;
import com.fasterxml.jackson.annotation.*;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.UUID;
import com.datastax.driver.core.utils.UUIDs;
import org.joda.time.DateTimeZone;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * Base class for all conversation events.
 */
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = ConversationCreatedEvent.class, name = "ConversationCreatedEvent"),
        @Type(value = MessageAddedEvent.class, name = "MessageAddedEvent"),
        @Type(value = MessageFilteredEvent.class, name = "MessageFilteredEvent"),
        @Type(value = MessageModeratedEvent.class, name = "MessageModeratedEvent"),
        @Type(value = MessageTerminatedEvent.class, name = "MessageTerminatedEvent"),
        @Type(value = ConversationClosedEvent.class, name = "ConversationClosedEvent"),
        @Type(value = ConversationDeletedEvent.class, name = "ConversationDeletedEvent"),
        @Type(value = CustomValueAddedEvent.class, name = "CustomValueAddedEvent")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ConversationEvent {

    // Not to be confused with eventid from Cassandra, as this one is different!
    private final String eventId;
    private final DateTime conversationModifiedAt;
    
    private UUID eventTimeUUID;

    /**
     * marks the version of this data format. can be used in future to handle old and new data formats.
     */
    @JsonProperty(required = false)
    private int formatVer = 1; // NOSONAR

    protected ConversationEvent(String eventId, DateTime conversationModifiedAt) {
        Assert.notNull(eventId);
        Assert.notNull(conversationModifiedAt);
        this.eventId = eventId;
        this.conversationModifiedAt = conversationModifiedAt;
        this.eventTimeUUID = UUIDs.timeBased();
    }

    public DateTime getConversationModifiedAt() {
        return conversationModifiedAt;
    }

    /**
     * @return conversation wide unique event id
     */
    public String getEventId() {
        return eventId;
    }

    protected static String eventIdForMessage(Class eventType, String messageId, DateTime conversationModifiedAt) {
        return String.format("%s-%s-%d", eventType.getSimpleName(), messageId, conversationModifiedAt.getMillis());
    }

    @JsonIgnore
    public UUID getEventTimeUUID() {
        return eventTimeUUID;
    }

    public void setEventTimeUUID(UUID eventTimeUUID) {
        this.eventTimeUUID = eventTimeUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationEvent that = (ConversationEvent) o;

        if (this.conversationModifiedAt.getMillis() != that.conversationModifiedAt.getMillis()) return false;
        if (!eventId.equals(that.eventId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = eventId.hashCode();
        result = 31 * result + conversationModifiedAt.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventId", eventId)
                .add("timestamp", conversationModifiedAt.toDateTime(DateTimeZone.UTC))
                .add("ver", formatVer).toString();
    }
}
