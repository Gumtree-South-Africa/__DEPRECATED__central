package com.ecg.messagebox.model;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConversationThread {

    private String id;
    private String adId;

    private Visibility visibility;
    private MessageNotification messageNotification;

    private Participant meParticipant;
    private Participant otherParticipant;

    private Message latestMessagePreview;

    private int numUnreadMessages;

    private BlockedUserInfo blockedUserInfo;

    private List<Message> messages = new ArrayList<>();

    public ConversationThread(String id, String adId,
                              Visibility visibility, MessageNotification messageNotification,
                              Participant meParticipant, Participant otherParticipant,
                              Message latestMessagePreview) {
        this.id = id;
        this.adId = adId;
        this.visibility = visibility;
        this.messageNotification = messageNotification;
        this.meParticipant = meParticipant;
        this.otherParticipant = otherParticipant;
        this.latestMessagePreview = latestMessagePreview;
    }

    public ConversationThread(ConversationThread conversation) {
        this(conversation.getId(), conversation.getAdId(),
                conversation.getVisibility(), conversation.getMessageNotification(),
                conversation.getMeParticipant(), conversation.getOtherParticipant(),
                conversation.getLatestMessagePreview());
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

    public Participant getMeParticipant() {
        return meParticipant;
    }

    public Participant getOtherParticipant() {
        return otherParticipant;
    }

    public Message getLatestMessagePreview() {
        return latestMessagePreview;
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

    public Optional<List<Message>> getMessages() {
        return Optional.ofNullable(messages);
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
                Objects.equal(id, that.id) &&
                Objects.equal(adId, that.adId) &&
                visibility == that.visibility &&
                messageNotification == that.messageNotification &&
                Objects.equal(meParticipant, that.meParticipant) &&
                Objects.equal(otherParticipant, that.otherParticipant) &&
                Objects.equal(latestMessagePreview, that.latestMessagePreview) &&
                Objects.equal(blockedUserInfo, that.blockedUserInfo) &&
                Objects.equal(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, adId, visibility, messageNotification, meParticipant, otherParticipant,
                latestMessagePreview, numUnreadMessages, blockedUserInfo, messages);
    }
}