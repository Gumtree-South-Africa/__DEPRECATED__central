package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.UnreadCountsResponse;
import com.ecg.messagebox.model.UserUnreadCounts;
import org.springframework.stereotype.Component;

@Component
public class UnreadCountsResponseConverter {

    public UnreadCountsResponse toUnreadCountsResponse(UserUnreadCounts unreadCounts) {
        return new UnreadCountsResponse(
                unreadCounts.getUserId(),
                unreadCounts.getNumUnreadConversations(),
                unreadCounts.getNumUnreadMessages()
        );
    }
}