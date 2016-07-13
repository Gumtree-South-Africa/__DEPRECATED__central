package com.ecg.messagebox.persistence.cassandra.model;

import java.util.List;

public class PostBoxIndex {

    private String postBoxId;
    private List<ConversationIndex> conversationIndices;

    public PostBoxIndex(String postBoxId, List<ConversationIndex> conversationIndices) {
        this.postBoxId = postBoxId;
        this.conversationIndices = conversationIndices;
    }

    public String getPostBoxId() {
        return postBoxId;
    }

    public List<ConversationIndex> getConversationIndices() {
        return conversationIndices;
    }
}