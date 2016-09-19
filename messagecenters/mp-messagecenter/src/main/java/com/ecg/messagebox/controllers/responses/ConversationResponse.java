package com.ecg.messagebox.controllers.responses;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ConversationResponse {

    private final String id;
    private final String adId;
    private final String visibility;
    private final String messageNotification;
    private final List<ParticipantResponse> participants;
    private final MessageResponse latestMessage;
    private final String creationDate;
    private final String emailSubject;
    private final int unreadMessagesCount;
    private final Optional<List<MessageResponse>> messages;

    public ConversationResponse(String id,
                                String adId,
                                String visibility,
                                String messageNotification,
                                List<ParticipantResponse> participants,
                                MessageResponse latestMessage,
                                String creationDate,
                                String emailSubject,
                                int unreadMessagesCount,
                                Optional<List<MessageResponse>> messages) {
        this.id = id;
        this.adId = adId;
        this.visibility = visibility;
        this.messageNotification = messageNotification;
        this.participants = participants;
        this.latestMessage = latestMessage;
        this.creationDate = creationDate;
        this.emailSubject = emailSubject;
        this.unreadMessagesCount = unreadMessagesCount;
        this.messages = messages;
    }

    public String getId() {
        return id;
    }

    public String getAdId() {
        return adId;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getMessageNotification() {
        return messageNotification;
    }

    public List<ParticipantResponse> getParticipants() {
        return participants;
    }

    public MessageResponse getLatestMessage() {
        return latestMessage;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public List<MessageResponse> getMessages() {
        return messages.isPresent() ? messages.get() : null;
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
                && unreadMessagesCount == that.unreadMessagesCount
                && Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, adId, visibility, messageNotification, participants,
                latestMessage, creationDate, emailSubject, unreadMessagesCount, messages);
    }
}