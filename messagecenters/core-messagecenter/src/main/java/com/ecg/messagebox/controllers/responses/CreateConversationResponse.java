package com.ecg.messagebox.controllers.responses;

public class CreateConversationResponse {
    private final boolean isExisting;
    private final String conversationId;

    public CreateConversationResponse(boolean isExisting, String conversationId) {
        this.isExisting = isExisting;
        this.conversationId = conversationId;
    }

    public boolean isExisting() {
        return isExisting;
    }

    public String getConversationId() {
        return conversationId;
    }
}
