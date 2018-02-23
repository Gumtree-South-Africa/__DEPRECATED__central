package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.util.TimeFormatUtils;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.swagger.annotations.ApiModelProperty;
import org.joda.time.DateTime;

public class ResponseDataResponse {

    @ApiModelProperty(required = true)
    private final String userId;
    @ApiModelProperty(required = true)
    private final String conversationId;
    @ApiModelProperty(required = true)
    private final int responseSpeed;
    @ApiModelProperty(required = true, example = TimeFormatUtils.DATE_FORMAT_STR_ISO8601_Z)
    @JsonSerialize(using = TimeFormatUtils.DateTimeSerializer.class)
    private final DateTime conversationCreationDate;
    @ApiModelProperty(required = true)
    private final MessageType conversationType;

    public ResponseDataResponse(ResponseData persistenceResponseData) {
        this.userId = persistenceResponseData.getUserId();
        this.conversationId = persistenceResponseData.getConversationId();
        this.responseSpeed = persistenceResponseData.getResponseSpeed();
        this.conversationCreationDate = persistenceResponseData.getConversationCreationDate();
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