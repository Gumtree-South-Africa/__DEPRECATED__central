package com.ecg.messagebox.resources.responses;

import io.swagger.annotations.ApiModelProperty;

public class PostMessageResponse {

    @ApiModelProperty(required = true)
    private String correlationId;

    public PostMessageResponse(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
