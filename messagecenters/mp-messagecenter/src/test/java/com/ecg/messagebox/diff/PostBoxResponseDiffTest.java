package com.ecg.messagebox.diff;

import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.lang.String.format;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxResponseDiffTest {

    private static final String USER_ID = "123";

    private static final PostBoxListItemResponse PB_ITEM_1 = new PostBoxListItemResponse(
            "c1", "buyerName", "sellerName", 1L, 2L, "m123",
            ConversationRole.Buyer, 3, new MessageResponse("msgId", "2016-02-01T16:21:21.071+01:00", MailTypeRts.INBOUND, "text", "senderEmail", "asq"),
            "2016-02-01T15:21:21.071+01:00"
    );

    private static final PostBoxListItemResponse PB_ITEM_1_WTH_DIFF = new PostBoxListItemResponse(
            "c1", "buyerName", "sellerName2", 1L, 2L, "m123",
            ConversationRole.Buyer, 3, new MessageResponse("msgId", "2016-02-01T16:23:21.071+01:00", MailTypeRts.INBOUND, "text", "senderEmail", "asq"),
            "2016-02-01T15:21:21.071+01:00"
    );

    private static final PostBoxListItemResponse PB_ITEM_2 = new PostBoxListItemResponse(
            "c2", "buyerName", "sellerName", 1L, 2L, "m123",
            ConversationRole.Buyer, 3, new MessageResponse("msgId", "2016-02-01T17:21:21.071+01:00", MailTypeRts.INBOUND, "text", "senderEmail", "asq"),
            "2016-02-01T15:21:21.071+01:00"
    );
    private static final PostBoxListItemResponse PB_ITEM_3 = new PostBoxListItemResponse(
            "c3", "buyerName", "sellerName", 1L, 2L, "m123",
            ConversationRole.Buyer, 3, new MessageResponse("msgId", "2016-02-01T18:21:21.071+01:00", MailTypeRts.INBOUND, "text", "senderEmail", "asq"),
            "2016-02-01T15:21:21.071+01:00"
    );

    @Mock
    private DiffReporter reporter;

    private PostBoxResponseDiff diff;

    @Before
    public void setup() {
        diff = new PostBoxResponseDiff(reporter, true, true);
    }

    @Test
    public void postBoxResponse_noDiff() {
        PostBoxResponse oldResponse = new PostBoxResponse();
        oldResponse.initNumUnreadMessages(3);
        oldResponse.meta(1, 0, 50);
        oldResponse.addItem(PB_ITEM_1);

        PostBoxResponse newResponse = new PostBoxResponse();
        newResponse.initNumUnreadMessages(3);
        newResponse.meta(1, 0, 50);
        newResponse.addItem(PB_ITEM_1);

        diff.diff(USER_ID, newResponse, oldResponse);

        verifyZeroInteractions(reporter);
    }

    @Test
    public void postBoxResponse_convInOldModelOnly() {
        PostBoxResponse oldResponse = new PostBoxResponse();
        oldResponse.initNumUnreadMessages(3);
        oldResponse.meta(1, 0, 50);
        oldResponse.addItem(PB_ITEM_1);
        oldResponse.addItem(PB_ITEM_2);
        oldResponse.addItem(PB_ITEM_3);

        PostBoxResponse newResponse = new PostBoxResponse();
        newResponse.initNumUnreadMessages(3);
        newResponse.meta(1, 0, 50);
        newResponse.addItem(PB_ITEM_1);

        diff.diff(USER_ID, newResponse, oldResponse);

        verify(reporter).report(
                format("postBoxResponseDiff(%s) - 2 conversations in old model only - %s", USER_ID, "c3, c2"),
                false
        );
    }

    @Test
    public void postBoxResponse_convInNewModelOnly() {
        PostBoxResponse oldResponse = new PostBoxResponse();
        oldResponse.initNumUnreadMessages(3);
        oldResponse.meta(1, 0, 50);
        oldResponse.addItem(PB_ITEM_1);

        PostBoxResponse newResponse = new PostBoxResponse();
        newResponse.initNumUnreadMessages(3);
        newResponse.meta(1, 0, 50);
        newResponse.addItem(PB_ITEM_1);
        newResponse.addItem(PB_ITEM_2);
        newResponse.addItem(PB_ITEM_3);

        diff.diff(USER_ID, newResponse, oldResponse);

        verify(reporter).report(
                format("postBoxResponseDiff(%s) - 2 conversations in new model only - %s", USER_ID, "c3, c2"),
                false
        );
    }

    @Test
    public void postBoxResponse_diff() {
        PostBoxResponse oldResponse = new PostBoxResponse();
        oldResponse.initNumUnreadMessages(3);
        oldResponse.meta(1, 0, 50);
        oldResponse.addItem(PB_ITEM_1);

        PostBoxResponse newResponse = new PostBoxResponse();
        newResponse.initNumUnreadMessages(3);
        newResponse.meta(1, 0, 50);
        newResponse.addItem(PB_ITEM_1_WTH_DIFF);
        newResponse.addItem(PB_ITEM_2);

        diff.diff(USER_ID, newResponse, oldResponse);

        verify(reporter).report(
                format("postBoxResponseDiff(%s) - 1 conversations in new model only - %s", USER_ID, "c2"),
                false
        );
        verify(reporter).report(
                format("postBoxResponseDiff(%s) - asq - conversations[c1](0).sellerName - new: '%s' vs old: '%s'", USER_ID, "sellerName2", "sellerName"),
                true
        );
        verify(reporter).report(
                format("postBoxResponseDiff(%s) - asq - conversations[c1](0).receivedDate - new: '%s' vs old: '%s'", USER_ID, "2016-02-01T16:23:21.071+01:00", "2016-02-01T16:21:21.071+01:00"),
                true
        );
        verifyNoMoreInteractions(reporter);
    }
}