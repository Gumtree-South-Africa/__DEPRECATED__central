package com.ecg.messagebox.controllers.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SystemMessagePayload {
    @NotNull
    private String text;

    @NotNull
    private String customData;

    @NotNull
    @NotEmpty
    private String adId;

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
}

