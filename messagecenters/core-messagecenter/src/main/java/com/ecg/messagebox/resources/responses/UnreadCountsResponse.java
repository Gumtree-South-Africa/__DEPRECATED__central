package com.ecg.messagebox.resources.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnreadCountsResponse {

    @ApiModelProperty(required = true)
    private final String userId;
    @ApiModelProperty(required = true)
    private final int conversationsWithUnreadMessagesCount;
    @ApiModelProperty(required = true)
    private final int unreadMessagesCount;

    public UnreadCountsResponse(String userId, int conversationsWithUnreadMessagesCount, int unreadMessagesCount) {
        this.userId = userId;
        this.conversationsWithUnreadMessagesCount = conversationsWithUnreadMessagesCount;
        this.unreadMessagesCount = unreadMessagesCount;
    }

    public String getUserId() {
        return userId;
    }

    public int getConversationsWithUnreadMessagesCount() {
        return conversationsWithUnreadMessagesCount;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnreadCountsResponse that = (UnreadCountsResponse) o;
        return Objects.equals(userId, that.userId)
                && conversationsWithUnreadMessagesCount == that.conversationsWithUnreadMessagesCount
                && unreadMessagesCount == that.unreadMessagesCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conversationsWithUnreadMessagesCount, unreadMessagesCount);
    }
}