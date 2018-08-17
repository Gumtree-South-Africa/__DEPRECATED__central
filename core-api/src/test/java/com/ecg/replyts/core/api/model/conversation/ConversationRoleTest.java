package com.ecg.replyts.core.api.model.conversation;

import com.ecg.comaas.events.Conversation.Participant.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConversationRoleTest {

    private static final String BUYER_EMAIL = "buyer@example.com";

    @Test
    public void getBuyerRole() {
        Conversation conversation = mock(Conversation.class);
        when(conversation.getBuyerId()).thenReturn(BUYER_EMAIL);

        assertEquals(ConversationRole.Buyer, ConversationRole.getRole(BUYER_EMAIL, conversation));
    }

    @Test
    public void getSellerRole() {
        Conversation conversation = mock(Conversation.class);
        when(conversation.getBuyerId()).thenReturn(BUYER_EMAIL);

        assertEquals(ConversationRole.Seller, ConversationRole.getRole("not-buyer@example.com", conversation));
    }

    @Test
    public void getBuyerParticipantRole() {
        assertEquals(Role.BUYER, ConversationRole.Buyer.getParticipantRole());
    }

    @Test
    public void getSellerParticipantRole() {
        assertEquals(Role.SELLER, ConversationRole.Seller.getParticipantRole());
    }
}