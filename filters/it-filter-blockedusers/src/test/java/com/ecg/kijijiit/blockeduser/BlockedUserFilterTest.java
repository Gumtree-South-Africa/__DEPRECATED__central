package com.ecg.kijijiit.blockeduser;

import com.ecg.kijijiit.blockeduser.zapi.ZapiUserStateService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by ddallemule on 2/10/14.
 */
@RunWith(MockitoJUnitRunner.class) public class BlockedUserFilterTest {

    @Mock private MessageProcessingContext messageProcessingContext;

    @Mock private Conversation conversation;

    @Mock private ZapiUserStateService userStateService;

    public static String TEST_USER = "test@test.com";


    @Before public void setUp() throws Exception {
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(conversation.getUserIdFor(any(ConversationRole.class))).thenReturn(TEST_USER);
        when(messageProcessingContext.getMessageDirection())
                        .thenReturn(MessageDirection.SELLER_TO_BUYER);

    }

    @Test public void testBlocked() throws Exception {
        when(userStateService.isBlocked(TEST_USER)).thenReturn(true);
        BlockedUserFilter blockedUserFilter = new BlockedUserFilter(userStateService);
        List<FilterFeedback> filterFeedbackList =
                        blockedUserFilter.filter(messageProcessingContext);
        assertEquals(1, filterFeedbackList.size());
        assertEquals(FilterResultState.DROPPED, filterFeedbackList.get(0).getResultState());
    }

    @Test public void testUnblocked() throws Exception {
        when(userStateService.isBlocked(TEST_USER)).thenReturn(false);
        BlockedUserFilter blockedUserFilter = new BlockedUserFilter(userStateService);
        List<FilterFeedback> filterFeedbackList =
                        blockedUserFilter.filter(messageProcessingContext);
        assertEquals(0, filterFeedbackList.size());
    }

    @Test public void testExceptionRetrievingUser() throws Exception {
        when(userStateService.isBlocked(TEST_USER)).thenThrow(new Exception("Test exception"));
        BlockedUserFilter blockedUserFilter = new BlockedUserFilter(userStateService);
        List<FilterFeedback> filterFeedbackList =
                        blockedUserFilter.filter(messageProcessingContext);
        assertEquals(0, filterFeedbackList.size());
    }
}
