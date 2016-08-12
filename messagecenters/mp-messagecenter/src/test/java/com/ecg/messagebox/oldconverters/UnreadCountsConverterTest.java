package com.ecg.messagebox.oldconverters;

import com.ecg.messagebox.model.UserUnreadCounts;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UnreadCountsConverterTest {

    private static final String USER_ID = "123";
    private static final int NUM_UNREAD_CONVERSATIONS = 2;
    private static final int NUM_UNREAD_MESSAGES = 5;

    private OldUnreadCountsConverter unreadCountsConverter = new OldUnreadCountsConverter();

    @Test
    public void toOldUnreadCounts() {
        com.ecg.messagecenter.persistence.PostBoxUnreadCounts expected =
                new com.ecg.messagecenter.persistence.PostBoxUnreadCounts(USER_ID, NUM_UNREAD_CONVERSATIONS, NUM_UNREAD_MESSAGES);

        UserUnreadCounts newUnreadCounts = new UserUnreadCounts(USER_ID, NUM_UNREAD_CONVERSATIONS, NUM_UNREAD_MESSAGES);
        com.ecg.messagecenter.persistence.PostBoxUnreadCounts actual = unreadCountsConverter.toOldUnreadCounts(newUnreadCounts);

        assertThat(actual, is(expected));
    }
}