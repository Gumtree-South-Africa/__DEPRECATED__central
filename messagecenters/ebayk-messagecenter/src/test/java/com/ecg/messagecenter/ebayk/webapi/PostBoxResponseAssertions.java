package com.ecg.messagecenter.ebayk.webapi;

import com.ecg.messagecenter.ebayk.webapi.responses.PostBoxListItemResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ProcessingStatus;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.ecg.sync.PostBoxResponse;

import java.util.Arrays;
import java.util.List;

import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.AD_ID;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.BUYER_ID;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.BUYER_NAME;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.DETAILED_CONVERSATION;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.NOW;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.SELLER_ID;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxOverviewControllerIntegrationTest.SELLER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

class PostBoxResponseAssertions {

    static void assertProcessingStatusOk(ResponseObject<PostBoxResponse> response) {
        ProcessingStatus status = response.getStatus();
        assertEquals(RequestState.OK, status.getState());
        assertNull(status.getDetails());
        assertNull(status.getErrorLog());
    }

    static void assertUnreadMessages(ResponseObject<PostBoxResponse> response, int expected) {
        PostBoxResponse body = response.getBody();
        assertEquals(expected, (int) body.getNumUnread());
    }

    static void assertConversationsSize(ResponseObject<PostBoxResponse> response, int expected) {
        PostBoxResponse body = response.getBody();
        assertEquals(expected, body.getConversations().size());
    }

    static void assertConversationsOriginalOrder(ResponseObject<PostBoxResponse> response) {
        assertConversationsOrder(response, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    static void assertConversationsOrder(ResponseObject<PostBoxResponse> response, Integer... conversationIds) {
        List<PostBoxListItemResponse> actualConversations = response.getBody().getConversations();
        List<Integer> expectedConversations = Arrays.asList(conversationIds);

        for (int i = 0; i < conversationIds.length; i++) {
            PostBoxListItemResponse actualConversation = actualConversations.get(i);
            Integer actual = getIndexFromConversationId(actualConversation.getId());
            Integer expected = expectedConversations.get(i);

            assertEquals(String.format("Conversation has a wrong order, expected [%s], actual [%s]", expected, actual), expected, actual);
        }
    }

    static void assertDetailConversation(PostBoxListItemResponse conversation, String email, ConversationRole role, MailTypeRts direction) {
        assertEquals(role, conversation.getRole());
        assertEquals(direction, conversation.getBoundness());
        assertEquals(email, conversation.getEmail());
        assertEquals(DETAILED_CONVERSATION, conversation.getId());
        assertEquals(BUYER_NAME, conversation.getBuyerName());
        assertEquals(SELLER_NAME, conversation.getSellerName());
        assertEquals(BUYER_ID, conversation.getUserIdBuyer());
        assertEquals(SELLER_ID, conversation.getUserIdSeller());
        assertEquals(AD_ID, conversation.getAdId());
        assertEquals(NOW.toString(), conversation.getReceivedDate());
        assertEquals("Message-Preview", conversation.getTextShortTrimmed());
        assertEquals(false, conversation.isUnread());
    }

    private static int getIndexFromConversationId(String conversationId) {
        return Integer.parseInt(conversationId.split("-")[1]);
    }
}
