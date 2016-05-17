package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;

/**
 * Response that contains postBox messages and conversations counters.
 */
public class PostBoxUnreadCountsResponse {

    private final long numUnreadMessages;
    private final long numUnreadConversations;

    public PostBoxUnreadCountsResponse(PostBoxUnreadCounts postBoxUnreadCounts) {
        this.numUnreadMessages = postBoxUnreadCounts.getNumUnreadMessages();
        this.numUnreadConversations = postBoxUnreadCounts.getNumUnreadConversations();
    }

    public long getNumUnreadMessages() {
        return numUnreadMessages;
    }

    public long getNumUnreadConversations() {
        return numUnreadConversations;
    }
}
