package com.ecg.messagebox.service.delegator;

import com.ecg.messagebox.service.PostBoxServiceDelegator;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PostBoxDelegator test class which verifies behaviour when:
 * <ul>
 * <li>read/write operations for old data model is enabled;
 * <li>diff tool is disabled;
 * <l1>data from the old data model is being returned to the caller.
 * </ul>
 */
public class OldEnabledUseOldDelegatorTest extends BaseDelegatorTest {

    private PostBoxServiceDelegator delegator = new PostBoxServiceDelegator(oldPbService, newPbService,
            pbRespConverter, convRespConverter, unreadCountsConverter, diff, newModelConfig, diffConfig, true, MESSAGES_LIMIT,
            CORE_POOL_SIZE, MAX_POOL_SIZE, DIFF_CORE_POOL_SIZE, DIFF_MAX_POOL_SIZE, DIFF_MAX_QUEUE_SIZE);

    @Before
    public void setup() {
        when(newModelConfig.newModelEnabled(anyString())).thenReturn(false);
        when(newModelConfig.useNewModel(anyString())).thenReturn(false);
        when(diffConfig.useDiff(anyString())).thenReturn(false);
    }

    @Test
    public void processNewMessage() {
        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyZeroInteractions(newPbService);
        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
    }

    @Test
    public void processNewMessageWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService)
                .processNewMessage(anyString(), any(Conversation.class), any(Message.class), any(ConversationRole.class), anyBoolean());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyZeroInteractions(newPbService);
        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
    }

    @Test
    public void getConversation() {
        when(oldPbService.getConversation(anyString(), anyString())).thenReturn(Optional.of(convResponse));

        Optional<ConversationResponse> actual = delegator.getConversation(USER_ID, CONV_ID);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).getConversation(USER_ID, CONV_ID);
        assertThat(actual.get(), is(convResponse));
    }

    @Test
    public void getConversationWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService).getConversation(anyString(), anyString());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.getConversation(USER_ID, CONV_ID);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).getConversation(USER_ID, CONV_ID);
    }

    @Test
    public void markConversationAsRead() {
        when(oldPbService.markConversationAsRead(anyString(), anyString())).thenReturn(Optional.of(convResponse));

        Optional<ConversationResponse> actual = delegator.markConversationAsRead(USER_ID, CONV_ID);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).markConversationAsRead(USER_ID, CONV_ID);
        assertThat(actual.get(), is(convResponse));
    }

    @Test
    public void markConversationAsReadWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService).markConversationAsRead(anyString(), anyString());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.markConversationAsRead(USER_ID, CONV_ID);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).markConversationAsRead(USER_ID, CONV_ID);
    }

    @Test
    public void getConversations() {
        when(oldPbService.getConversations(anyString(), anyInt(), anyInt())).thenReturn(pbResponse);

        PostBoxResponse actual = delegator.getConversations(USER_ID, SIZE, PAGE);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).getConversations(USER_ID, SIZE, PAGE);
        assertThat(actual, is(pbResponse));
    }

    @Test
    public void getConversationsWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService).getConversations(USER_ID, SIZE, PAGE);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.getConversations(USER_ID, SIZE, PAGE);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).getConversations(USER_ID, SIZE, PAGE);
    }

    @Test
    public void deleteConversations() {
        when(oldPbService.deleteConversations(anyString(), anyListOf(String.class), anyInt(), anyInt())).thenReturn(pbResponse);

        PostBoxResponse actual = delegator.deleteConversations(USER_ID, CONV_IDS, SIZE, PAGE);

        verifyZeroInteractions(newPbService, diff);

        verifyWithTimeout(oldPbService).deleteConversations(USER_ID, CONV_IDS, SIZE, PAGE);
        assertThat(actual, is(pbResponse));
    }

    @Test
    public void deleteConversationsWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService).deleteConversations(anyString(), anyListOf(String.class), anyInt(), anyInt());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.deleteConversations(USER_ID, CONV_IDS, SIZE, PAGE);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).deleteConversations(USER_ID, CONV_IDS, SIZE, PAGE);
    }

    @Test
    public void getUnreadCounts() {
        when(oldPbService.getUnreadCounts(anyString())).thenReturn(oldPbUnreadCounts);

        PostBoxUnreadCounts actual = delegator.getUnreadCounts(USER_ID);

        verifyZeroInteractions(newPbService, diff);

        verifyWithTimeout(oldPbService).getUnreadCounts(USER_ID);
        assertThat(actual, is(oldPbUnreadCounts));
    }

    @Test
    public void getUnreadCountsWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService).getUnreadCounts(anyString());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_OLD);

        delegator.getUnreadCounts(USER_ID);

        verifyZeroInteractions(newPbService, diff);
        verifyWithTimeout(oldPbService).getUnreadCounts(USER_ID);
    }
}