package com.ecg.messagebox.diff;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.StringJoiner;

import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Buyer;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversationResponseDiffTest {

    private static final String USER_ID = "1";
    private static final String CONVERSATION_ID = "c1";

    private static final ConversationResponse CONV_RESP = new ConversationResponse(
            "c1", Buyer, "buyerEmail", "sellerEmail", "buyerName", "sellerName", 1L, 2L, "m123",
            "2016-02-01T15:21:21.071+01:00", "emailSubject",
            newArrayList(
                    new MessageResponse("msgId1", "2016-02-01T15:21:21.071+01:00", MailTypeRts.OUTBOUND, "msg text", "senderEmail", "asq"),
                    new MessageResponse("msgId2", "2016-02-01T15:21:21.071+01:00", MailTypeRts.INBOUND, "msg text 2", "senderEmail2", "asq")
            ),
            3
    );

    private static final ConversationResponse CONV_RESP_WITH_DIFF_SELLER_EMAIL = new ConversationResponse(
            "c1", Buyer, "buyerEmail", "sellerEmail2", "buyerName", "sellerName", 1L, 2L, "m123",
            "2016-02-01T15:21:21.071+01:00", "emailSubject",
            newArrayList(
                    new MessageResponse("msgId1", "2016-02-01T15:21:21.071+01:00", MailTypeRts.OUTBOUND, "msg text", "senderEmail", "asq"),
                    new MessageResponse("msgId2", "2016-02-01T15:21:21.071+01:00", MailTypeRts.INBOUND, "msg text 2", "senderEmail2", "asq")
            ),
            3
    );

    private static final ConversationResponse CONV_RESP_WITH_DIFF_MSGS_SIZE = new ConversationResponse(
            "c1", Buyer, "buyerEmail", "sellerEmail", "buyerName", "sellerName", 1L, 2L, "m123",
            "2016-02-01T15:21:21.071+01:00", "emailSubject",
            newArrayList(
                    new MessageResponse("msgId2", "2016-02-01T15:21:21.071+01:00", MailTypeRts.INBOUND, "msg text 2", "senderEmail2", "asq")
            ),
            3
    );

    private static final ConversationResponse CONV_RESP_WITH_DIFF_MSG_RECEIVED_DATE = new ConversationResponse(
            "c1", Buyer, "buyerEmail", "sellerEmail", "buyerName", "sellerName", 1L, 2L, "m123",
            "2016-02-01T15:21:21.071+01:00", "emailSubject",
            newArrayList(
                    new MessageResponse("msgId1", "2016-02-01T15:23:21.071+01:00", MailTypeRts.OUTBOUND, "msg text", "senderEmail", "asq"),
                    new MessageResponse("msgId2", "2016-02-01T15:21:21.071+01:00", MailTypeRts.INBOUND, "msg text 2", "senderEmail2", "asq")
            ),
            3
    );

    @Mock
    private DiffReporter reporter;
    @Mock
    private Conversation rtsConversation;
    @Mock
    private UserIdentifierService userIdentifierService;

    private ConversationResponseDiff diff;

    @Before
    public void setup() {
        diff = new ConversationResponseDiff(reporter, true);
    }

    @Test
    public void conversationResponse_noDiff() {
        diff.diff(USER_ID, CONVERSATION_ID, of(CONV_RESP), of(CONV_RESP));

        verifyZeroInteractions(reporter);
    }

    @Test
    public void conversationResponse_emptyOldConversationResponse() {
        String params = new StringJoiner(",").add(USER_ID).add(CONVERSATION_ID).toString();

        diff.diff(USER_ID, CONVERSATION_ID, of(CONV_RESP), empty());

        verify(reporter).report(
                format("conversationResponseDiff(%s) - conversation responses are missing - new: 'no' vs old: 'yes'", params),
                false
        );
        verifyNoMoreInteractions(reporter);
    }

    @Test
    public void conversationResponse_emptyNewConversationResponse() {
        String params = new StringJoiner(",").add(USER_ID).add(CONVERSATION_ID).toString();

        diff.diff(USER_ID, CONVERSATION_ID, empty(), of(CONV_RESP));

        verify(reporter).report(
                format("conversationResponseDiff(%s) - conversation responses are missing - new: 'yes' vs old: 'no'", params),
                false
        );
        verifyNoMoreInteractions(reporter);
    }

    @Test
    public void conversationResponse_diffSellerEmail() {
        String params = new StringJoiner(",").add(USER_ID).add(CONVERSATION_ID).toString();

        diff.diff(USER_ID, CONVERSATION_ID, of(CONV_RESP_WITH_DIFF_SELLER_EMAIL), of(CONV_RESP));

        verify(reporter).report(
                format("conversationResponseDiff(%s) - sellerEmail - new: 'sellerEmail2' vs old: 'sellerEmail'", params),
                true
        );
        verifyNoMoreInteractions(reporter);
    }

    @Test
    public void conversationResponse_diffMessagesSize() {
        String params = new StringJoiner(",").add(USER_ID).add(CONVERSATION_ID).toString();

        diff.diff(USER_ID, CONVERSATION_ID, of(CONV_RESP_WITH_DIFF_MSGS_SIZE), of(CONV_RESP));

        verify(reporter).report(
                format("conversationResponseDiff(%s) - messages size - new: '1' vs old: '2'", params),
                true
        );
        verifyNoMoreInteractions(reporter);
    }

    @Test
    public void conversationResponse_diffReceivedDate() {
        String params = new StringJoiner(",").add(USER_ID).add(CONVERSATION_ID).toString();

        diff.diff(USER_ID, CONVERSATION_ID, of(CONV_RESP_WITH_DIFF_MSG_RECEIVED_DATE), of(CONV_RESP));

        verify(reporter).report(
                format("conversationResponseDiff(%s) - asq - messages[msgId1](0).receivedDate - new: '2016-02-01T15:23:21.071+01:00' vs old: '2016-02-01T15:21:21.071+01:00'", params),
                true
        );
        verifyNoMoreInteractions(reporter);
    }
}