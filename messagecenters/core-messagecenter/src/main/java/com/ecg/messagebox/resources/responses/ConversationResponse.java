package com.ecg.messagebox.resources.responses;

import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.util.TimeFormatUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponse {

    @ApiModelProperty(required = true)
    private final String id;
    @ApiModelProperty(required = true)
    private final String adId;
    @ApiModelProperty(required = true)
    private final Visibility visibility;
    @ApiModelProperty(required = true)
    private final MessageNotification messageNotification;
    @ApiModelProperty(required = true)
    private final List<ParticipantResponse> participants;
    @ApiModelProperty(required = true)
    private final MessageResponse latestMessage;
    @ApiModelProperty(required = true, example = TimeFormatUtils.DATE_FORMAT_STR_ISO8601_Z)
    @JsonSerialize(using = TimeFormatUtils.DateTimeSerializer.class)
    private final DateTime creationDate;
    @ApiModelProperty(required = true)
    private final String emailSubject;
    @ApiModelProperty(required = true)
    private final int unreadMessagesCount;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<MessageResponse> messages;
    private final String imageUrl;
    private final String title;

    public ConversationResponse(String id,
                                String adId,
                                Visibility visibility,
                                MessageNotification messageNotification,
                                List<ParticipantResponse> participants,
                                MessageResponse latestMessage,
                                DateTime creationDate,
                                String emailSubject,
                                String title,
                                String imageUrl,
                                int unreadMessagesCount,
                                List<MessageResponse> messages) {
        this.id = id;
        this.adId = adId;
        this.visibility = visibility;
        this.messageNotification = messageNotification;
        this.participants = participants;
        this.latestMessage = latestMessage;
        this.creationDate = creationDate;
        this.emailSubject = emailSubject;
        this.title = title;
        this.unreadMessagesCount = unreadMessagesCount;
        this.messages = messages;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getAdId() {
        return adId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public MessageNotification getMessageNotification() {
        return messageNotification;
    }

    public List<ParticipantResponse> getParticipants() {
        return participants;
    }

    public MessageResponse getLatestMessage() {
        return latestMessage;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public String getTitle() {
        return title;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationResponse that = (ConversationResponse) o;
        return Objects.equals(id, that.id)
                && Objects.equals(adId, that.adId)
                && Objects.equals(visibility, that.visibility)
                && Objects.equals(messageNotification, that.messageNotification)
                && Objects.equals(participants, that.participants)
                && Objects.equals(latestMessage, that.latestMessage)
                && Objects.equals(creationDate, that.creationDate)
                && Objects.equals(emailSubject, that.emailSubject)
                && Objects.equals(title, that.title)
                && Objects.equals(imageUrl, that.imageUrl)
                && unreadMessagesCount == that.unreadMessagesCount
                && Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, adId, visibility, messageNotification, participants,
                latestMessage, creationDate, emailSubject, title, imageUrl, unreadMessagesCount, messages);
    }
}