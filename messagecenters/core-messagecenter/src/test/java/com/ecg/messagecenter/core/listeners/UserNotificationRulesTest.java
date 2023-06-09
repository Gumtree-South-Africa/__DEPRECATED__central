package com.ecg.messagecenter.core.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * User: maldana
 * Date: 28.10.13
 * Time: 15:10
 *
 * @author maldana@ebay.de
 */
public class UserNotificationRulesTest {


    private UserNotificationRules notificationRules;
    private Message message;

    @Before
    public void setUp() {
        notificationRules = new UserNotificationRules();

        message = mock(Message.class);
    }

    @Test
    public void correctNotificationDirectionBasedOnMessageDirection() {
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        assertTrue(notificationRules.sellerShouldBeNotified(message.getState(), message.getMessageDirection()));
        assertFalse(notificationRules.buyerShouldBeNotified(message.getState(), message.getMessageDirection()));
    }

    @Test
    public void correctNotificationDirectionBasedOnMessageDirection2() {
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        assertFalse(notificationRules.sellerShouldBeNotified(message.getState(), message.getMessageDirection()));
        assertTrue(notificationRules.buyerShouldBeNotified(message.getState(), message.getMessageDirection()));
    }

    @Test
    public void doNotSentToBuyerOrSellerIfNotInSENTState(){
        when(message.getState()).thenReturn(MessageState.BLOCKED);

        assertFalse(notificationRules.sellerShouldBeNotified(message.getState(), message.getMessageDirection()));
        assertFalse(notificationRules.buyerShouldBeNotified(message.getState(), message.getMessageDirection()));
    }

}
