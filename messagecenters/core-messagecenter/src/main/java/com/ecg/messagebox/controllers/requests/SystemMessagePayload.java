package com.ecg.messagebox.controllers.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemMessagePayload {

    @NotNull(message = "System message text cannot be null")
    private String text;

    @NotNull(message = "System message custom data cannot be null")
    private String customData;

    @NotEmpty(message = "System message Ad ID cannot be empty")
    private String adId;

    private boolean sendPush = false;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCustomData() {
        return customData;
    }

    public void setCustomData(String customData) {
        this.customData = customData;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public boolean isSendPush() {
        return sendPush;
    }

    public void setSendPush(boolean sendPush) {
        this.sendPush = sendPush;
    }
}

