package com.ecg.messagecenter.persistence;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Keeps the number of unread conversations and messages for a postbox.
 */
public class PostBoxUnreadCounts {

    private final String userId;
    private final int numUnreadConversations;
    private final int numUnreadMessages;

    public PostBoxUnreadCounts(String userId, int numUnreadConversations, int numUnreadMessages) {
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
        PostBoxUnreadCounts that = (PostBoxUnreadCounts) o;
        return Objects.equals(userId, that.userId)
                && numUnreadConversations == that.numUnreadConversations
                && numUnreadMessages == that.numUnreadMessages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, numUnreadConversations, numUnreadMessages);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("numUnreadConversations", numUnreadConversations)
                .add("numUnreadMessages", numUnreadMessages)
                .toString();
    }
}
