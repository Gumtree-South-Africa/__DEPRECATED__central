package com.ecg.messagebox.oldconverters;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OldPostBoxResponseConverterTest {

    private static final int MSG_TEXT_MAX_CHARS = 10;

    private static final String USER_ID = "1";

    private static final int PAGE = 1;
    private static final int SIZE = 100;

    private static final String C1_ID = "c1", C2_ID = "c2";
    private static final String C1_AD_ID = "m1", C2_AD_ID = "m2";

    private static final int C1_UNREAD_MSG_COUNT = 3, C2_UNREAD_MSG_COUNT = 6;
    private static final int UNREAD_MSG_COUNT = C1_UNREAD_MSG_COUNT + C2_UNREAD_MSG_COUNT;

    private static final String C1_BUYER_ID = USER_ID, C1_SELLER_ID = "2", C2_BUYER_ID = "3", C2_SELLER_ID = USER_ID;
    private static final String C1_BUYER_NAME = "name 1", C2_BUYER_NAME = "name 3", C2_SELLER_NAME = "name 1";

    private static final MessageResponse MSG_RESP_WITH_LONG_TXT =
            new MessageResponse("receivedDate1", MailTypeRts.INBOUND, "a very long text", "senderEmail1", "asq");
    private static final MessageResponse MSG_RESP_WITH_SHORT_TXT =
            new MessageResponse("receivedDate2", MailTypeRts.OUTBOUND, "short text", "senderEmail2", "asq");

    @Mock
    private OldMessageResponseConverter msgRespConverterMock;

    private OldPostBoxResponseConverter postBoxResponseConverter;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        postBoxResponseConverter = new OldPostBoxResponseConverter(msgRespConverterMock, MSG_TEXT_MAX_CHARS);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void toPostBoxResponse() {
        // conversation 1
        List<Participant> c1Participants = asList(
                new Participant(C1_BUYER_ID, C1_BUYER_NAME, "email", ParticipantRole.BUYER),
                new Participant(C1_SELLER_ID, null, "email", ParticipantRole.SELLER));
        Message c1LatestMsg = new Message(UUIDs.timeBased(), MessageType.BID, new MessageMetadata(C1_BUYER_ID, "c1 text"));
        ConversationThread c1 = new ConversationThread(C1_ID, C1_AD_ID, Visibility.ACTIVE, MessageNotification.RECEIVE,
                c1Participants, c1LatestMsg, new ConversationMetadata(now(), "email subject")
        ).addNumUnreadMessages(C1_UNREAD_MSG_COUNT);

        // conversation 2
        List<Participant> c2Participants = asList(
                new Participant(C2_BUYER_ID, C2_BUYER_NAME, "email", ParticipantRole.BUYER),
                new Participant(C2_SELLER_ID, C2_SELLER_NAME, "email", ParticipantRole.SELLER));
        Message c2LatestMsg = new Message(UUIDs.timeBased(), MessageType.CHAT, new MessageMetadata(C2_BUYER_ID, "c2 text"));
        ConversationThread c2 = new ConversationThread(C2_ID, C2_AD_ID, Visibility.ACTIVE, MessageNotification.MUTE,
                c2Participants, c2LatestMsg, new ConversationMetadata(now(), "email subject")
        ).addNumUnreadMessages(C2_UNREAD_MSG_COUNT);

        MessageResponse msgRespWithTruncatedText = new MessageResponse(
                MSG_RESP_WITH_LONG_TXT.getReceivedDate(),
                MSG_RESP_WITH_LONG_TXT.getBoundness(),
                "a very...",
                MSG_RESP_WITH_LONG_TXT.getSenderEmail(),
                "asq"
        );
        PostBoxResponse expected = new PostBoxResponse()
                .initNumUnreadMessages(UNREAD_MSG_COUNT)
                .meta(2, PAGE, SIZE)
                .addItem(new PostBoxListItemResponse(C1_ID, C1_BUYER_NAME, "",
                        Long.valueOf(C1_BUYER_ID), Long.valueOf(C1_SELLER_ID), C1_AD_ID,
                        ConversationRole.Buyer, C1_UNREAD_MSG_COUNT, msgRespWithTruncatedText,
                        toFormattedTimeISO8601ExplicitTimezoneOffset(now())))
                .addItem(new PostBoxListItemResponse(C2_ID, C2_BUYER_NAME, C2_SELLER_NAME,
                        Long.valueOf(C2_BUYER_ID), Long.valueOf(C2_SELLER_ID), C2_AD_ID,
                        ConversationRole.Seller, C2_UNREAD_MSG_COUNT, MSG_RESP_WITH_SHORT_TXT,
                        toFormattedTimeISO8601ExplicitTimezoneOffset(now())));

        when(msgRespConverterMock.toMessageResponse(c1LatestMsg, USER_ID, c1Participants)).thenReturn(MSG_RESP_WITH_LONG_TXT);
        when(msgRespConverterMock.toMessageResponse(c2LatestMsg, USER_ID, c2Participants)).thenReturn(MSG_RESP_WITH_SHORT_TXT);

        PostBox postBox = new PostBox(USER_ID, Arrays.asList(c1, c2), new UserUnreadCounts(USER_ID, 2, UNREAD_MSG_COUNT), 10);
        PostBoxResponse actual = postBoxResponseConverter.toPostBoxResponse(postBox, PAGE, SIZE);

        verify(msgRespConverterMock).toMessageResponse(c1LatestMsg, USER_ID, c1Participants);
        verify(msgRespConverterMock).toMessageResponse(c2LatestMsg, USER_ID, c2Participants);
        assertThat(actual, is(expected));
    }
}