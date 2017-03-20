package com.ecg.messagebox.model;

import java.util.Objects;

/**
 * Keeps the number of unread messages and conversations for a user.
 */
public class UserUnreadCounts {

    private String userId;
    private int numUnreadConversations;
    private int numUnreadMessages;

    public UserUnreadCounts(String userId, int numUnreadConversations, int numUnreadMessages) {
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