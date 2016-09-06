package com.ecg.messagebox.service.delegator;

import com.ecg.messagebox.service.PostBoxServiceDelegator;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * PostBoxDelegator test class which verifies behaviour when:
 * <ul>
 * <li>read/write operations for both old and new data model are enabled;
 * <li>diff tool is enabled;
 * <l1>data from the new data model is being returned to the caller.
 * </ul>
 */
public class OldNewDiffEnabledUseNewDelegatorTest extends BaseDelegatorTest {

    private PostBoxServiceDelegator delegator = new PostBoxServiceDelegator(oldPbService, newPbService,
            pbRespConverter, convRespConverter, unreadCountsConverter, diff, newModelConfig, diffConfig, true, MESSAGES_LIMIT,
            CORE_POOL_SIZE, MAX_POOL_SIZE, DIFF_CORE_POOL_SIZE, DIFF_MAX_POOL_SIZE, DIFF_MAX_QUEUE_SIZE);

    @Before
    public void setup() {
        when(newModelConfig.newModelEnabled(anyString())).thenReturn(true);
        when(newModelConfig.useNewModel(anyString())).thenReturn(true);
        when(diffConfig.useDiff(anyString())).thenReturn(true);
    }

    @Test
    public void processNewMessage() throws InterruptedException {
        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
        verifyWithTimeout(newPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);
    }

    @Test
    public void processNewMessageWithExceptionFromOld() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService)
                .processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
        verifyWithTimeout(newPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);
    }

    @Test
    public void processNewMessageWithExceptionFromNew() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_NEW))
                .when(newPbService)
                .processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_NEW);

        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
        verifyWithTimeout(newPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);
    }

    @Test
    public void processNewMessageWithExceptionFromBoth() {
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_OLD))
                .when(oldPbService)
                .processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
        doThrow(new RuntimeException(EXPECTED_ERROR_MSG_FROM_NEW))
                .when(newPbService)
                .processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(EXPECTED_ERROR_MSG_FROM_NEW);

        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyWithTimeout(oldPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);
        verifyWithTimeout(newPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);
    }
}