package com.ecg.messagecenter.persistence;

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
}
