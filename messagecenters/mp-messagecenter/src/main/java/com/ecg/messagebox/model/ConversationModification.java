package com.ecg.messagebox.model;

import org.joda.time.DateTime;

import java.util.UUID;

public class ConversationModification {
    private final String userId;
    private final String conversationId;
    private final String advertisementId;
    private final UUID messageId;
    private final DateTime modifiedAt;

    public ConversationModification(String userId, String conversationId, String advertisementId, UUID messageId, DateTime modifiedAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.advertisementId = advertisementId;
        this.messageId = messageId;
        this.modifiedAt = modifiedAt;
    }

    public ConversationModification(String userId, String conversationId, UUID messageId, DateTime modifiedAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.advertisementId = null;
        this.messageId = messageId;
        this.modifiedAt = modifiedAt;
    }

    public String getUserId() {
        return userId;
    }

    public DateTime getModifiedAt() { return modifiedAt; }

    public String getConversationId() {
        return conversationId;
    }

    public String getAdvertisementId() {
        return advertisementId;
    }

    public UUID getMessageId() { return messageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationModification that = (ConversationModification) o;

        if (!userId.equals(that.userId)) return false;
        if (!conversationId.equals(that.conversationId)) return false;
        if (advertisementId != null ? !advertisementId.equals(that.advertisementId) : that.advertisementId != null) return false;
        if (!messageId.equals(that.messageId)) return false;
        return modifiedAt.equals(that.modifiedAt);

    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + conversationId.hashCode();
        result = 31 * result + (advertisementId != null ? advertisementId.hashCode() : 0);
        result = 31 * result + messageId.hashCode();
        result = 31 * result + modifiedAt.hashCode();
        return result;
    }
}
