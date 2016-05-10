package com.ebay.ecg.replyts.robot.api.requests.payload;

import java.util.List;

/**
 * Created by mdarapour.
 */
public class ResponsePayload {
    private String status;
    private List<String> errors;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
