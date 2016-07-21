package com.ecg.messagebox.model;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public class PostBox {

    private String id;
    private List<ConversationThread> conversations;
    private PostBoxUnreadCounts unreadCounts;

    public PostBox(String id, List<ConversationThread> conversations, PostBoxUnreadCounts unreadCounts) {
        this.id = id;
        this.conversations = conversations;
        this.unreadCounts = unreadCounts;
    }

    public String getId() {
        return id;
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
        return Objects.equals(id, postBox.id)
                && Objects.equals(conversations, postBox.conversations)
                && Objects.equals(unreadCounts, postBox.unreadCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, conversations, unreadCounts);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("conversationThreads", conversations)
                .add("unreadCounts", unreadCounts)
                .toString();
    }
}