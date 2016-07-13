package com.ecg.messagebox.persistence.cassandra.model;

import com.ecg.messagebox.model.Visibility;

import java.util.UUID;

public class ConversationIndex {

    private String conversationId;
    private String adId;
    private Visibility visibility;
    private UUID latestMessageId;

    public ConversationIndex(String conversationId, String adId, Visibility visibility, UUID latestMessageId) {
        this.conversationId = conversationId;
        this.adId = adId;
        this.visibility = visibility;
        this.latestMessageId = latestMessageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getAdId() {
        return adId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public UUID getLatestMessageId() {
        return latestMessageId;
    }
}