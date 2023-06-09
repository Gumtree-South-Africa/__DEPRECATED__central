package com.ecg.messagebox.resources.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationsResponse {

    @ApiModelProperty(required = true)
    private final String userId;
    @ApiModelProperty(required = true)
    private final int unreadMessagesCount;
    @ApiModelProperty(required = true)
    private final int conversationsWithUnreadMessagesCount;
    @ApiModelProperty(required = true)
    private final int offset;
    @ApiModelProperty(required = true)
    private final int limit;
    @ApiModelProperty(required = true)
    private final int totalCount;
    private final List<ConversationResponse> conversations;

    public ConversationsResponse(String userId, int unreadMessagesCount, int conversationsWithUnreadMessagesCount,
                                 List<ConversationResponse> conversations, int offset, int limit, int totalCount) {
        this.userId = userId;
        this.unreadMessagesCount = unreadMessagesCount;
        this.conversationsWithUnreadMessagesCount = conversationsWithUnreadMessagesCount;
        this.conversations = conversations;
        this.offset = offset;
        this.limit = limit;
        this.totalCount = totalCount;
    }

    public String getUserId() {
        return userId;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public int getConversationsWithUnreadMessagesCount() {
        return conversationsWithUnreadMessagesCount;
    }

    public List<ConversationResponse> getConversations() {
        return conversations;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationsResponse that = (ConversationsResponse) o;
        return Objects.equals(userId, that.userId)
                && unreadMessagesCount == that.unreadMessagesCount
                && conversationsWithUnreadMessagesCount == that.conversationsWithUnreadMessagesCount
                && Objects.equals(conversations, that.conversations)
                && offset == that.offset
                && limit == that.limit
                && totalCount == that.totalCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, unreadMessagesCount, conversationsWithUnreadMessagesCount,
                conversations, offset, limit, totalCount);
    }
}