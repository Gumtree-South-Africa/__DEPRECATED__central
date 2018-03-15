package com.ecg.messagebox.resources.responses;

public class PostMessageResponse {

    private String messageId;

    public PostMessageResponse(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
}
