package com.ecg.messagebox.model;

import org.joda.time.DateTime;

import java.util.Objects;
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

        return Objects.equals(this.userId, that.userId) &&
                Objects.equals(this.conversationId, that.conversationId) &&
                Objects.equals(this.advertisementId, that.advertisementId) &&
                Objects.equals(this.messageId, that.messageId) &&
                Objects.equals(this.modifiedAt, that.modifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conversationId, advertisementId, messageId, modifiedAt);
    }
}
