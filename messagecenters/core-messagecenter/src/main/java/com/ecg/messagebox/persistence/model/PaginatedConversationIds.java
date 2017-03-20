package com.ecg.messagebox.persistence.model;

import java.util.List;
import java.util.Objects;

public class PaginatedConversationIds {

    private List<String> conversationIds;
    private int conversationsTotalCount;

    public PaginatedConversationIds(List<String> conversationIds, int conversationsTotalCount) {
        this.conversationIds = conversationIds;
        this.conversationsTotalCount = conversationsTotalCount;
    }

    public List<String> getConversationIds() {
        return conversationIds;
    }

    public int getConversationsTotalCount() {
        return conversationsTotalCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginatedConversationIds that = (PaginatedConversationIds) o;
        return Objects.equals(conversationIds, that.conversationIds)
                && conversationsTotalCount == that.conversationsTotalCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationIds, conversationsTotalCount);
    }
}