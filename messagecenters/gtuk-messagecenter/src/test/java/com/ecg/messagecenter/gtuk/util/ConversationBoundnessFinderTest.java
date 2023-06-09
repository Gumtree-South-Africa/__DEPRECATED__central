package com.ecg.messagecenter.gtuk.util;

import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ConversationBoundnessFinderTest {
    @Test
    public void mailToSellerIsInboundWhenMessageDirectionIsBuyerToSeller() {
        Assert.assertEquals(MailTypeRts.INBOUND, ConversationBoundnessFinder.boundnessForRole(ConversationRole.Seller, MessageDirection.BUYER_TO_SELLER));
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
