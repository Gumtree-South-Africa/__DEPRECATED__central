package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageMetadata {

    private String text;
    private String senderUserId;
    private String customData;
    private Map<String,String> headers;
    private List<Attachment> attachments;

    @JsonCreator
    public MessageMetadata(@JsonProperty("text") String text,
                           @JsonProperty("senderUserId") String senderUserId,
                           @JsonProperty("customData") String customData,
                           @JsonProperty("headers") Map<String, String> headers,
                           @JsonProperty("attachments") List<Attachment> attachments) {
        this.text = text;
        this.senderUserId = senderUserId;
        this.customData = customData;
        this.headers = headers;
        this.attachments = attachments;
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

    public List<Attachment> getAttachments() {
        return attachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMetadata metadata = (MessageMetadata) o;
        return Objects.equals(text, metadata.text)
                && Objects.equals(senderUserId, metadata.senderUserId)
                && Objects.equals(customData, metadata.customData)
                && Objects.equals(headers, metadata.headers)
                && Objects.equals(attachments, metadata.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, senderUserId, customData);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("text", text)
                .add("senderUserId", senderUserId)
                .add("customData", customData)
                .add("headers", headers)
                .add("attachments", attachments)
                .toString();
    }
}
