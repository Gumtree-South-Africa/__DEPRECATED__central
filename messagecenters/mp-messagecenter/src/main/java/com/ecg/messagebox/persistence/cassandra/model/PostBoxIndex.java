package com.ecg.messagebox.persistence.cassandra.model;

import java.util.List;

public class PostBoxIndex {

    private String userId;
    private List<ConversationIndex> conversationIndices;

    public PostBoxIndex(String userId, List<ConversationIndex> conversationIndices) {
        this.userId = userId;
        this.conversationIndices = conversationIndices;
    }

    public String getUserId() {
        return userId;
    }

    public List<ConversationIndex> getConversationIndices() {
        return conversationIndices;
    }
}