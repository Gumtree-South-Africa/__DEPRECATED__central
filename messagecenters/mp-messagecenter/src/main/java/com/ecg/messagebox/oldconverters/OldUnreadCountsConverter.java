package com.ecg.messagebox.oldconverters;

import com.ecg.messagebox.model.UserUnreadCounts;
import org.springframework.stereotype.Component;

@Component
public class OldUnreadCountsConverter {

    public com.ecg.messagecenter.persistence.PostBoxUnreadCounts toOldUnreadCounts(UserUnreadCounts newUnreadCounts) {
        return new com.ecg.messagecenter.persistence.PostBoxUnreadCounts(
                newUnreadCounts.getUserId(),
                newUnreadCounts.getNumUnreadConversations(),
                newUnreadCounts.getNumUnreadMessages());
    }
}