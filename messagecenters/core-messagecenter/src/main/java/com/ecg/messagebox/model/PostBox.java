package com.ecg.messagebox.model;

import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public class PostBox {

    private final String userId;
    private final List<ConversationThread> conversations;
    private final UserUnreadCounts unreadCounts;
    private final int conversationsTotalCount;

    public PostBox(String userId, List<ConversationThread> conversations, UserUnreadCounts unreadCounts, int conversationsTotalCount) {
        this.userId = userId;
        this.conversations = conversations;
        this.unreadCounts = unreadCounts;
        this.conversationsTotalCount = conversationsTotalCount;
    }

    public String getUserId() {
        return userId;
    }

    public List<ConversationThread> getConversations() {
        return conversations;
    }

    public UserUnreadCounts getUnreadCounts() {
        return unreadCounts;
    }

    public int getConversationsTotalCount() {
        return conversationsTotalCount;
    }

    public PostBox removeConversations(List<String> conversationIds) {
        conversations.removeIf(conv -> conversationIds.contains(conv.getId()));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBox postBox = (PostBox) o;
        return Objects.equals(userId, postBox.userId)
                && Objects.equals(conversations, postBox.conversations)
                && Objects.equals(unreadCounts, postBox.unreadCounts)
                && conversationsTotalCount == postBox.conversationsTotalCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conversations, unreadCounts, conversationsTotalCount);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("conversations", conversations)
                .add("unreadCounts", unreadCounts)
                .add("conversationsTotalCount", conversationsTotalCount)
                .toString();
    }
}
