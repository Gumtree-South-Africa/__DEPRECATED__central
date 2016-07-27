package com.ecg.messagecenter.persistence;

/**
 * Keeps the number of unread conversations and messages for a postbox.
 */
public class PostBoxUnreadCounts {

    private final int numUnreadConversations;
    private final int numUnreadMessages;

    public PostBoxUnreadCounts(int numUnreadConversations, int numUnreadMessages) {
        this.numUnreadConversations = numUnreadConversations;
        this.numUnreadMessages = numUnreadMessages;
    }

    public int getNumUnreadConversations() {
        return numUnreadConversations;
    }

    public int getNumUnreadMessages() {
        return numUnreadMessages;
    }
}
