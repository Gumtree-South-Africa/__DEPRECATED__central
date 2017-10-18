package com.ecg.replyts.core.api.model.conversation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Keeps the number of unread messages and conversations for a user.
 */
public class UserUnreadCounts {

    private String userId;
    private int numUnreadConversations;
    private int numUnreadMessages;
    /**
     * Marks the version of this data format.
     */
    @JsonProperty(required = false)
    private int formatVer = 1;

    @JsonCreator
    public UserUnreadCounts(@JsonProperty("userId") String userId,
                            @JsonProperty("numUnreadConversations") int numUnreadConversations,
                            @JsonProperty("numUnreadMessages") int numUnreadMessages) {
        this.userId = userId;
        this.numUnreadConversations = numUnreadConversations;
        this.numUnreadMessages = numUnreadMessages;
    }

    public String getUserId() {
        return userId;
    }

    public int getNumUnreadConversations() {
        return numUnreadConversations;
    }

    public int getNumUnreadMessages() {
        return numUnreadMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserUnreadCounts that = (UserUnreadCounts) o;
        return Objects.equals(userId, that.userId)
                && numUnreadConversations == that.numUnreadConversations
                && numUnreadMessages == that.numUnreadMessages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, numUnreadConversations, numUnreadMessages);
    }
}