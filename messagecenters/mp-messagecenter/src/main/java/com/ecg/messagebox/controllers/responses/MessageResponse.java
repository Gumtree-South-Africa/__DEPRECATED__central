package com.ecg.messagebox.controllers.responses;

import java.util.Objects;

public class MessageResponse {

    private final String id;
    private final String type;
    private final String text;
    private final String senderUserId;
    private final String receivedDate;
    private final String customData;

    public MessageResponse(String id, String type, String text, String senderUserId,
                           String receivedDate, String customData) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.senderUserId = senderUserId;
        this.receivedDate = receivedDate;
        this.customData = customData;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public String getCustomData() {
        return customData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageResponse that = (MessageResponse) o;
        return Objects.equals(id, that.id)
                && Objects.equals(type, that.type)
                && Objects.equals(text, that.text)
                && Objects.equals(senderUserId, that.senderUserId)
                && Objects.equals(receivedDate, that.receivedDate)
                && Objects.equals(customData, that.customData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, text, senderUserId, receivedDate, customData);
    }
}