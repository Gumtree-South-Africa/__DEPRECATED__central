package com.ecg.messagebox.resources.responses;

import io.swagger.annotations.ApiModelProperty;

public class CreateConversationResponse {

    @ApiModelProperty(required = true)
    private final boolean isExisting;

    @ApiModelProperty(required = true)
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
