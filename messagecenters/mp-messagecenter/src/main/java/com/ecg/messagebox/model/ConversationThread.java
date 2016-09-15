package com.ecg.messagebox.model;

import com.ecg.replyts.core.runtime.persistence.BlockedUserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ConversationThread {

    private String id;
    private String adId;
    private Visibility visibility;
    private MessageNotification messageNotification;
    private List<Participant> participants;
    private Message latestMessage;
    private ConversationMetadata metadata;

    private int numUnreadMessages;
    private BlockedUserInfo blockedUserInfo;
    private List<Message> messages = new ArrayList<>();

    public ConversationThread(String id, String adId,
                              Visibility visibility, MessageNotification messageNotification,
                              List<Participant> participants,
                              Message latestMessage, ConversationMetadata metadata) {
        this.id = id;
        this.adId = adId;
        this.visibility = visibility;
        this.messageNotification = messageNotification;
        this.participants = participants;
        this.latestMessage = latestMessage;
        this.metadata = metadata;
    }

    public ConversationThread(ConversationThread conversation) {
        this(conversation.getId(), conversation.getAdId(),
                conversation.getVisibility(), conversation.getMessageNotification(),
                conversation.getParticipants(),
                conversation.getLatestMessage(),
                conversation.getMetadata());
        this.numUnreadMessages = conversation.numUnreadMessages;
        this.blockedUserInfo = conversation.blockedUserInfo;
        this.messages = conversation.messages;
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

    public int getNumUnreadMessages() {
        return numUnreadMessages;
    }

    public ConversationThread addNumUnreadMessages(int numUnreadMessages) {
        this.numUnreadMessages = numUnreadMessages;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationThread that = (ConversationThread) o;
        return numUnreadMessages == that.numUnreadMessages &&
                Objects.equals(id, that.id) &&
                Objects.equals(adId, that.adId) &&
                visibility == that.visibility &&
                messageNotification == that.messageNotification &&
                Objects.equals(participants, that.participants) &&
                Objects.equals(latestMessage, that.latestMessage) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(blockedUserInfo, that.blockedUserInfo) &&
                Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, adId, visibility, messageNotification, participants,
                latestMessage, metadata, numUnreadMessages, blockedUserInfo, messages);
    }
}