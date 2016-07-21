package com.ecg.messagebox.model;

import java.util.Objects;

/**
 * Keeps the number of unread messages and conversations for a postbox.
 */
public class PostBoxUnreadCounts {

    private String postBoxId;
    private int numUnreadMessages;
    private int numUnreadConversations;

    public PostBoxUnreadCounts(String postBoxId, int numUnreadConversations, int numUnreadMessages) {
        this.postBoxId = postBoxId;
        this.numUnreadMessages = numUnreadMessages;
        this.numUnreadConversations = numUnreadConversations;
    }

    public String getPostBoxId() {
        return postBoxId;
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
        return Objects.equals(postBoxId, that.postBoxId)
                && numUnreadMessages == that.numUnreadMessages
                && numUnreadConversations == that.numUnreadConversations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(postBoxId, numUnreadMessages, numUnreadConversations);
    }
}