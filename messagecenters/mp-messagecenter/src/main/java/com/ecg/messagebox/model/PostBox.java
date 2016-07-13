package com.ecg.messagebox.model;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public class PostBox {

    private String postBoxId;
    private List<ConversationThread> conversations;
    private PostBoxUnreadCounts unreadCounts;

    public PostBox(String postBoxId, List<ConversationThread> conversations, PostBoxUnreadCounts unreadCounts) {
        this.postBoxId = postBoxId;
        this.conversations = conversations;
        this.unreadCounts = unreadCounts;
    }

    public String getPostBoxId() {
        return postBoxId;
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
        return Objects.equals(postBoxId, postBox.postBoxId)
                && Objects.equals(conversations, postBox.conversations)
                && Objects.equals(unreadCounts, postBox.unreadCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postBoxId, conversations);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("postBoxId", postBoxId)
                .add("conversationThreads", conversations)
                .add("unreadCounts", unreadCounts)
                .toString();
    }
}