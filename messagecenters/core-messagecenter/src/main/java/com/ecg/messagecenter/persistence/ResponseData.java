package com.ecg.messagecenter.persistence;

import org.joda.time.DateTime;

import java.util.Objects;

public class ResponseData {

    private static final int DEFAULT_RESPONSE_SPEED = -1;

    private String userId;
    private String conversationId;
    private int responseSpeed;
    private DateTime conversationCreationDate;
    private MessageType conversationType;

    public ResponseData(String userId, String conversationId, DateTime conversationCreationDate, MessageType conversationType, int responseSpeed) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.responseSpeed = responseSpeed;
        this.conversationCreationDate = conversationCreationDate;
        this.conversationType = conversationType;
    }

    public ResponseData(String userId, String conversationId, DateTime conversationCreationDate, MessageType conversationType) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.responseSpeed = DEFAULT_RESPONSE_SPEED;
        this.conversationCreationDate = conversationCreationDate;
        this.conversationType = conversationType;
    }

    public String getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public int getResponseSpeed() {
        return responseSpeed;
    }

    public DateTime getConversationCreationDate() {
        return conversationCreationDate;
    }

    public MessageType getConversationType() {
        return conversationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseData that = (ResponseData) o;
        return responseSpeed == that.responseSpeed &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(conversationId, that.conversationId) &&
                Objects.equals(conversationCreationDate, that.conversationCreationDate) &&
                conversationType == that.conversationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conversationId, responseSpeed, conversationCreationDate, conversationType);
    }
}
