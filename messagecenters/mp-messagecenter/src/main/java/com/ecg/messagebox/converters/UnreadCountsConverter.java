package com.ecg.messagebox.converters;

import com.ecg.messagebox.model.PostBoxUnreadCounts;
import org.springframework.stereotype.Component;

@Component
public class UnreadCountsConverter {

    public com.ecg.messagecenter.persistence.PostBoxUnreadCounts toOldUnreadCounts(PostBoxUnreadCounts newUnreadCounts) {
        return new com.ecg.messagecenter.persistence.PostBoxUnreadCounts(
                newUnreadCounts.getUserId(),
                newUnreadCounts.getNumUnreadConversations(),
                newUnreadCounts.getNumUnreadMessages());
    }
}