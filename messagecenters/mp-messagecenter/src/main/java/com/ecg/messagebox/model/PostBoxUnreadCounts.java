package com.ecg.messagebox.model;

import java.util.Objects;

/**
 * Keeps the number of unread messages and conversations for a postbox.
 */
public class PostBoxUnreadCounts {

    private final int numUnreadMessages;
    private final int numUnreadConversations;

    public PostBoxUnreadCounts(int numUnreadConversations, int numUnreadMessages) {
        this.numUnreadMessages = numUnreadMessages;
        this.numUnreadConversations = numUnreadConversations;
    }

    public int getNumUnreadMessages() {
        return numUnreadMessages;
    }

    public int getNumUnreadConversations() {
        return numUnreadConversations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBoxUnreadCounts that = (PostBoxUnreadCounts) o;
        return numUnreadMessages == that.numUnreadMessages &&
                numUnreadConversations == that.numUnreadConversations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numUnreadMessages, numUnreadConversations);
    }
}