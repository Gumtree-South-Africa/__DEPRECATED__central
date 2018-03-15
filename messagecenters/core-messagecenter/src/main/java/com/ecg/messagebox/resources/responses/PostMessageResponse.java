package com.ecg.messagebox.resources.responses;

import io.swagger.annotations.ApiModelProperty;

public class PostMessageResponse {

    @ApiModelProperty(required = true)
    private String messageId;

    public PostMessageResponse(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
}
