package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.UnreadCountsResponse;
import com.ecg.messagebox.model.UserUnreadCounts;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UnreadCountsResponseConverterTest {

    private static final String USER_ID = "123";
    private static final int NUM_UNREAD_CONVERSATIONS = 2;
    private static final int NUM_UNREAD_MESSAGES = 5;

    private UnreadCountsResponseConverter responseConverter;

    @Before
    public void setup() {
        responseConverter = new UnreadCountsResponseConverter();
    }

    @Test
    public void toUnreadCountsResponse() {
        UserUnreadCounts newUnreadCounts = new UserUnreadCounts(USER_ID, NUM_UNREAD_CONVERSATIONS, NUM_UNREAD_MESSAGES);
        UnreadCountsResponse expected = new UnreadCountsResponse(USER_ID, NUM_UNREAD_CONVERSATIONS, NUM_UNREAD_MESSAGES);
        UnreadCountsResponse actual = responseConverter.toUnreadCountsResponse(newUnreadCounts);
        assertThat(actual, is(expected));
    }
}