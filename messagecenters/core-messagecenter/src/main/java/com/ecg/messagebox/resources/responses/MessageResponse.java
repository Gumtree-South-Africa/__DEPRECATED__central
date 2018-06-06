package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.util.TimeFormatUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    @ApiModelProperty(required = true)
    private final String id;
    @ApiModelProperty(required = true)
    private final MessageType type;
    @ApiModelProperty(required = true)
    private final String text;
    @ApiModelProperty(required = true, notes = "-1 if a sender is System (System Message)")
    private final String senderUserId;
    @ApiModelProperty(required = true, example = TimeFormatUtils.DATE_FORMAT_STR_ISO8601_Z)
    @JsonSerialize(using = TimeFormatUtils.DateTimeSerializer.class)
    private final DateTime receivedDate;
    @ApiModelProperty(required = true)
    private boolean isRead = true;
    private final String customData;
    private final Map<String,String> metaData;

    public MessageResponse(String id, MessageType type, String text, String senderUserId, DateTime receivedDate, String customData, Map<String,String> metaData) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.senderUserId = senderUserId;
        this.receivedDate = receivedDate;
        this.customData = customData;
        this.metaData = metaData;
    }

    public String getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public DateTime getReceivedDate() {
        return receivedDate;
    }

    public String getCustomData() {
        return customData;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public Map<String,String> getMetaData() { return metaData;}

    public MessageResponse withIsRead(boolean isRead) {
        this.isRead = isRead;
        return this;
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
                && Objects.equals(isRead, that.isRead)
                && Objects.equals(customData, that.customData)
                && Objects.equals(metaData, that.metaData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, text, senderUserId, receivedDate, isRead, customData, metaData);
    }
}