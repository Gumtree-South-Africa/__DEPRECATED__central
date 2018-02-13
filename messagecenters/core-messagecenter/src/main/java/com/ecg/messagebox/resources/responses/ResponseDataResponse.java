package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.model.MessageType;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ResponseDataResponse {

    private final String userId;
    private final String conversationId;
    private final int responseSpeed;
    private final String conversationCreationDate;
    private final MessageType conversationType;

    public ResponseDataResponse(com.ecg.messagebox.model.ResponseData persistenceResponseData) {
        this.userId = persistenceResponseData.getUserId();
        this.conversationId = persistenceResponseData.getConversationId();
        this.responseSpeed = persistenceResponseData.getResponseSpeed();
        this.conversationCreationDate = MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(persistenceResponseData.getConversationCreationDate());
        this.conversationType = persistenceResponseData.getConversationType();
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

    public String getConversationCreationDate() {
        return conversationCreationDate;
    }

    public MessageType getConversationType() {
        return conversationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseDataResponse that = (ResponseDataResponse) o;
        return responseSpeed == that.responseSpeed &&
                Objects.equal(userId, that.userId) &&
                Objects.equal(conversationId, that.conversationId) &&
                Objects.equal(conversationCreationDate, that.conversationCreationDate) &&
                conversationType == that.conversationType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, conversationId, responseSpeed, conversationCreationDate, conversationType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("conversationId", conversationId)
                .add("responseSpeed", responseSpeed)
                .add("conversationCreationDate", conversationCreationDate)
                .add("conversationType", conversationType)
                .toString();
    }
}