package com.ecg.messagebox.model;

import com.ecg.replyts.core.runtime.persistence.BlockedUserInfo;

import java.util.*;

public class ConversationThread {

    private String id;
    private String adId;
    private String userId;
    private Visibility visibility;
    private MessageNotification messageNotification;
    private List<Participant> participants;
    private Message latestMessage;
    private ConversationMetadata metadata;

    private BlockedUserInfo blockedUserInfo;
    private List<Message> messages = new ArrayList<>();
    private Map<String, Integer> participantUnreadMessages = new HashMap<>();

    public ConversationThread(String id, String adId, String userId,
                              Visibility visibility, MessageNotification messageNotification,
                              List<Participant> participants,
                              Message latestMessage, ConversationMetadata metadata) {
        this.id = id;
        this.adId = adId;
        this.userId = userId;
        this.visibility = visibility;
        this.messageNotification = messageNotification;
        this.participants = participants;
        this.latestMessage = latestMessage;
        this.metadata = metadata;
    }

    public ConversationThread(ConversationThread conversation) {
        this(conversation.getId(), conversation.getAdId(), conversation.getUserId(),
                conversation.getVisibility(), conversation.getMessageNotification(),
                conversation.getParticipants(),
                conversation.getLatestMessage(),
                conversation.getMetadata());
        this.participantUnreadMessages = conversation.participantUnreadMessages;
        this.blockedUserInfo = conversation.blockedUserInfo;
        this.messages = conversation.messages;
    }

    public String getId() {
        return id;
    }

    public String getAdId() {
        return adId;
    }

    public String getUserId() {
        return userId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public ConversationThread changeVisibility(Visibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public MessageNotification getMessageNotification() {
        return messageNotification;
    }

    public ConversationThread changeMessageNotification(MessageNotification messageNotification) {
        this.messageNotification = messageNotification;
        return this;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    public ConversationMetadata getMetadata() {
        return metadata;
    }

    public int getNumUnreadMessages(String userId) {
        return participantUnreadMessages.get(userId);
    }

    public ConversationThread addNumUnreadMessages(String userId, int numUnreadMessages) {
        this.participantUnreadMessages.put(userId, numUnreadMessages);
        return this;
    }

    public Optional<BlockedUserInfo> getBlockedUserInfo() {
        return Optional.ofNullable(blockedUserInfo);
    }

    public ConversationThread addBlockedUserInfo(BlockedUserInfo blockedUserInfo) {
        this.blockedUserInfo = blockedUserInfo;
        return this;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public ConversationThread addMessages(List<Message> messages) {
        this.messages = messages;
        return this;
    }

    public int getHighestOtherParticipantUnreadMessages() {
        int unreadMessages = 0;
        for (Participant participant : getParticipants()) {
            if (!participant.getUserId().equals(userId)) {
                int pUnreadMessages = participantUnreadMessages.get(participant.getUserId());
                if (pUnreadMessages > unreadMessages) {
                    unreadMessages = pUnreadMessages;
                }
            }
        }
        return unreadMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationThread that = (ConversationThread) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(adId, that.adId) &&
                Objects.equals(userId, that.userId) &&
                visibility == that.visibility &&
                messageNotification == that.messageNotification &&
                Objects.equals(participants, that.participants) &&
                Objects.equals(latestMessage, that.latestMessage) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(blockedUserInfo, that.blockedUserInfo) &&
                Objects.equals(messages, that.messages) &&
                Objects.equals(participantUnreadMessages, that.participantUnreadMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, adId, userId, visibility, messageNotification, participants,
                latestMessage, metadata, participantUnreadMessages, blockedUserInfo, messages);
    }

    @Override
    public String toString() {
        return "ConversationThread{" +
                "id='" + id + '\'' +
                ", adId='" + adId + '\'' +
                ", userId='" + userId + '\'' +
                ", visibility=" + visibility +
                ", messageNotification=" + messageNotification +
                ", participants=" + participants +
                ", latestMessage=" + latestMessage +
                ", metadata=" + metadata +
                ", participantUnreadMessages=" + participantUnreadMessages +
                ", blockedUserInfo=" + blockedUserInfo +
                ", messages=" + messages +
                '}';
    }
}