package com.ecg.messagecenter.util;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ConversationBoundnessFinderTest {
    @Test
    public void mailToSellerIsInboundWhenMessageDirectionIsBuyerToSeller() {
        MailTypeRts actual = ConversationBoundnessFinder.boundnessForRole(ConversationRole.Seller, MessageDirection.BUYER_TO_SELLER);
        assertThat(actual).isEqualTo(MailTypeRts.INBOUND);
    }

    @Test
    public void mailToBuyerIsInboundWhenMessageDirectionIsSellerToBuyer() {
        MailTypeRts actual = ConversationBoundnessFinder.boundnessForRole(ConversationRole.Buyer, MessageDirection.SELLER_TO_BUYER);
        assertThat(actual).isEqualTo(MailTypeRts.INBOUND);
    }

    @Test
    public void mailToSellerIsOutboundWhenMessageDirectionIsSellerToBuyer() {
        MailTypeRts actual = ConversationBoundnessFinder.boundnessForRole(ConversationRole.Seller, MessageDirection.SELLER_TO_BUYER);
        assertThat(actual).isEqualTo(MailTypeRts.OUTBOUND);
    }

    @Test
    public void mailToBuyerIsOutboundWhenMessageDirectionIsBuyerToSeller() {
        MailTypeRts actual = ConversationBoundnessFinder.boundnessForRole(ConversationRole.Buyer, MessageDirection.BUYER_TO_SELLER);
        assertThat(actual).isEqualTo(MailTypeRts.OUTBOUND);
    }

    @Test
    public void lookupUsersRole_whenNoBuyerId_shouldReturnNull() {
        String email = "some.email@company.domain";
        AbstractConversationThread abstractConversationThreadMock = mockConversationThread(Optional.empty());

        ConversationRole actual = ConversationBoundnessFinder.lookupUsersRole(email, abstractConversationThreadMock);
        assertThat(actual).isNull();
    }

    @Test
    public void lookupUsersRole_whenBuyerEmail_shouldReturnBuyer() {
        String email = "some.email@company.domain";
        AbstractConversationThread abstractConversationThreadMock = mockConversationThread(Optional.of(email));

        ConversationRole actual = ConversationBoundnessFinder.lookupUsersRole(email, abstractConversationThreadMock);
        assertThat(actual).isEqualTo(ConversationRole.Buyer);
    }

    @Test
    public void lookupUsersRole_whenSomeBuyerId_shouldReturnSeller() {
        String email = "some.email@company.domain";
        AbstractConversationThread abstractConversationThreadMock = mockConversationThread(Optional.of("some.other.email@company.domain"));

        ConversationRole actual = ConversationBoundnessFinder.lookupUsersRole(email, abstractConversationThreadMock);
        assertThat(actual).isEqualTo(ConversationRole.Seller);
    }

    private static AbstractConversationThread mockConversationThread(Optional<String> buyerEmail) {
        AbstractConversationThread abstractConversationThreadMock = mock(AbstractConversationThread.class);
        when(abstractConversationThreadMock.getBuyerId()).thenReturn(buyerEmail);
        return abstractConversationThreadMock;
    }
}
