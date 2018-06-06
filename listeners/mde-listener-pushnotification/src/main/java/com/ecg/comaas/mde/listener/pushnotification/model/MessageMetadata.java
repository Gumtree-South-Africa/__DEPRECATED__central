package com.ecg.comaas.mde.listener.pushnotification.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;

public class MessageMetadata {

    private String text;
    private String senderUserId;
    private String customData;
    private Map<String,String> headers;

    @JsonCreator
    public MessageMetadata(@JsonProperty("text") String text,
                           @JsonProperty("senderUserId") String senderUserId,
                           @JsonProperty("customData") String customData,
                           @JsonProperty("headers") Map<String,String> headers) {
        this.text = text;
        this.senderUserId = senderUserId;
        this.customData = customData;
        this.headers = headers;
    }

    public MessageMetadata(String text, String senderUserId, String customData) {
        this.text = text;
        this.senderUserId = senderUserId;
        this.customData = customData;
    }

    public MessageMetadata(String text, String senderUserId) {
        this.text = text;
        this.senderUserId = senderUserId;
    }

    public String getText() {
        return text;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public String getCustomData() {
        return customData;
    }

    public Map<String,String> getHeaders() { return headers;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMetadata metadata = (MessageMetadata) o;
        return Objects.equals(text, metadata.text)
                && Objects.equals(senderUserId, metadata.senderUserId)
                && Objects.equals(customData, metadata.customData)
                && Objects.equals(headers, metadata.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, senderUserId, customData, headers);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", text)
                .add("senderUserId", senderUserId)
                .add("customData", customData)
                .add("headers", headers)
                .toString();
    }
}
