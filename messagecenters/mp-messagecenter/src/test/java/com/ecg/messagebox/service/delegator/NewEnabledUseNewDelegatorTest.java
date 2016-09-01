package com.ecg.messagebox.service.delegator;

import com.ecg.messagebox.service.PostBoxServiceDelegator;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * PostBoxDelegator test class which verifies behaviour when:
 * <ul>
 * <li>read/write operations for new data model is enabled;
 * <li>diff tool is disabled;
 * <l1>data from the new data model is being returned to the caller.
 * </ul>
 */
public class NewEnabledUseNewDelegatorTest extends BaseDelegatorTest {

    private PostBoxServiceDelegator delegator = new PostBoxServiceDelegator(oldPbService, newPbService,
            pbRespConverter, convRespConverter, unreadCountsConverter, diff, newModelConfig, diffConfig, false, MESSAGES_LIMIT,
            CORE_POOL_SIZE, MAX_POOL_SIZE, DIFF_POOL_SIZE);

    @Before
    public void setup() {
        when(newModelConfig.newModelEnabled(anyString())).thenReturn(true);
        when(newModelConfig.useNewModel(anyString())).thenReturn(true);
        when(diffConfig.useDiff(anyString())).thenReturn(false);
    }

    @Test
    public void processNewMessage() {
        delegator.processNewMessage(USER_ID, rtsConversation, rtsMessage, CONV_ROLE, NEW_REPLY_ARRIVED);

        verifyZeroInteractions(oldPbService);
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

        verifyZeroInteractions(oldPbService);
        verifyWithTimeout(newPbService).processNewMessage(USER_ID, rtsConversation, rtsMessage, NEW_REPLY_ARRIVED);
    }
}