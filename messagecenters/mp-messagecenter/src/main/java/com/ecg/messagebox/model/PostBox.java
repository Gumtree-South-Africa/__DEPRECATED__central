package com.ecg.messagebox.model;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public class PostBox {

    private String userId;
    private List<ConversationThread> conversations;
    private PostBoxUnreadCounts unreadCounts;

    public PostBox(String userId, List<ConversationThread> conversations, PostBoxUnreadCounts unreadCounts) {
        this.userId = userId;
        this.conversations = conversations;
        this.unreadCounts = unreadCounts;
    }

    public String getUserId() {
        return userId;
    }

    public List<ConversationThread> getConversations() {
        return conversations;
    }

    public PostBoxUnreadCounts getUnreadCounts() {
        return unreadCounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBox postBox = (PostBox) o;
        return Objects.equals(userId, postBox.userId)
                && Objects.equals(conversations, postBox.conversations)
                && Objects.equals(unreadCounts, postBox.unreadCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conversations, unreadCounts);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("conversations", conversations)
                .add("unreadCounts", unreadCounts)
                .toString();
    }
}