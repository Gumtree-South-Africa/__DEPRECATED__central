package com.ecg.messagecenter.util;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 15:16
 *
 * @author maldana@ebay.de
 */
public class ConversationBoundnessFinderTest {

    @Test
    public void mailToSellerIsInboundWhenMessageDirectionIsBuyerToSeller() {
        assertEquals(MailTypeRts.INBOUND, ConversationBoundnessFinder.boundnessForRole(ConversationRole.Seller, MessageDirection.BUYER_TO_SELLER));
    }


    @Test
    public void mailToBuyerIsInboundWhenMessageDirectionIsSellerToBuyer() {
        assertEquals(MailTypeRts.INBOUND, ConversationBoundnessFinder.boundnessForRole(ConversationRole.Buyer, MessageDirection.SELLER_TO_BUYER));
    }


    @Test
    public void mailToSellerIsOutboundWhenMessageDirectionIsSellerToBuyer() {
        assertEquals(MailTypeRts.OUTBOUND, ConversationBoundnessFinder.boundnessForRole(ConversationRole.Seller, MessageDirection.SELLER_TO_BUYER));
    }


    @Test
    public void mailToBuyerIsOutboundWhenMessageDirectionIsBuyerToSeller() {
        assertEquals(MailTypeRts.OUTBOUND, ConversationBoundnessFinder.boundnessForRole(ConversationRole.Buyer, MessageDirection.BUYER_TO_SELLER));
    }

}
