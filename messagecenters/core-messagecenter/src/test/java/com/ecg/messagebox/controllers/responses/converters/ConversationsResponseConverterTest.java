package com.ecg.messagebox.controllers.responses.converters;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static com.ecg.messagebox.model.MessageNotification.MUTE;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationsResponseConverterTest {

    private static final int CONVS_TOTAL_COUNT = 3;
    private static final int CONV_WITH_UNREAD_MSGS_COUNT = 2;
    private static final int UNREAD_MSG_COUNT = 9;

    private static final String USER_ID_1 = "1", USER_ID_2 = "2";

    private static final int OFFSET = 0;
    private static final int LIMIT = 50;

    @Mock
    private ConversationResponse convRespMock;
    @Mock
    private ConversationResponseConverter convRespConverterMock;

    private ConversationsResponseConverter conversationsResponseConverter;

    @Before
    public void setup() {
        conversationsResponseConverter = new ConversationsResponseConverter(convRespConverterMock);
    }

    @Test
    public void toConversationsResponse() {
        // conversation 1
        ConversationThread c1 = new ConversationThread(
                "c1",
                "m1",
                ACTIVE,
                RECEIVE,
                emptyList(),
                new Message(UUIDs.timeBased(), MessageType.BID, new MessageMetadata(USER_ID_1, "text")),
                new ConversationMetadata(now(), "email subject", "title"));

        // conversation 2
        ConversationThread c2 = new ConversationThread(
                "c2",
                "m2",
                ACTIVE,
                MUTE,
                emptyList(),
                new Message(UUIDs.timeBased(), MessageType.CHAT, new MessageMetadata(USER_ID_2, "text")),
                new ConversationMetadata(now(), "email subject", "title"));

        // user's conversations
        PostBox postBox = new PostBox(USER_ID_1, Arrays.asList(c1, c2),
                new UserUnreadCounts(USER_ID_1, CONV_WITH_UNREAD_MSGS_COUNT, UNREAD_MSG_COUNT), CONVS_TOTAL_COUNT);

        ConversationsResponse expected = new ConversationsResponse(
                USER_ID_1,
                UNREAD_MSG_COUNT,
                CONV_WITH_UNREAD_MSGS_COUNT,
                Arrays.asList(convRespMock, convRespMock),
                OFFSET,
                LIMIT,
                CONVS_TOTAL_COUNT);

        when(convRespConverterMock.toConversationResponse(c1)).thenReturn(convRespMock);
        when(convRespConverterMock.toConversationResponse(c2)).thenReturn(convRespMock);

        ConversationsResponse actual = conversationsResponseConverter.toConversationsResponse(postBox, OFFSET, LIMIT);

        verify(convRespConverterMock).toConversationResponse(c1);
        verify(convRespConverterMock).toConversationResponse(c2);
        assertThat(actual, is(expected));
    }
}