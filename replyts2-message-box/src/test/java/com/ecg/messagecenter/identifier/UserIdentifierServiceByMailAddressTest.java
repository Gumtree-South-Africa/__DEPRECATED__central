package com.ecg.messagecenter.identifier;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by beckart on 15.10.15.
 */
public class UserIdentifierServiceByMailAddressTest {

    private Conversation conversation = mock(Conversation.class);

    private ConversationThread conversationThread = mock(ConversationThread.class);

    private UserIdentifierService userIdentifierService = new UserIdentifierServiceByMailAddress();

    @Before
    public void setup() {
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.de");
        when(conversation.getSellerId()).thenReturn("seller@seller.de");
    }

    @Test
    public void testGetBuyerUserId() throws Exception {
        String userId = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Buyer).get();
        assertThat(userId, is("buyer@buyer.de"));
    }

    @Test
    public void testGetSellerUserId() throws Exception {
        String userId = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Seller).get();
        assertThat(userId, is("seller@seller.de"));
    }

    @Test
    public void testSellerRole() {
        ConversationRole role = userIdentifierService.getRoleFromConversation("sellEr@seller.de", conversation);
        assertThat(role, is(ConversationRole.Seller));
    }

    @Test
    public void testBuyerRole() {
        ConversationRole role = userIdentifierService.getRoleFromConversation("buyeR@buyer.de", conversation);
        assertThat(role, is(ConversationRole.Buyer));
    }

    @Test
    public void testBuyerRoleForThread() {
        when(conversationThread.getBuyerId()).thenReturn(com.google.common.base.Optional.of("buyeR@buyer.de"));
        ConversationRole role = userIdentifierService.getRoleFromConversation("buyer@buyer.de", conversationThread);
        assertThat(role, is(ConversationRole.Buyer));
    }

    @Test
    public void testSellerRoleForThread() {
        when(conversationThread.getBuyerId()).thenReturn(com.google.common.base.Optional.of("buyeR@buyer.de"));
        ConversationRole role = userIdentifierService.getRoleFromConversation("selleR@seller.de", conversationThread);
        assertThat(role, is(ConversationRole.Seller));
    }

    @Test
    public void testGetBuyerId() {
        Optional<String> buyerUserId = userIdentifierService.getBuyerUserId(conversation);
        assertThat(buyerUserId.get(), is("buyer@buyer.de"));
    }

    @Test
    public void testGetSellerId() {
        Optional<String> sellerUserId = userIdentifierService.getSellerUserId(conversation);
        assertThat(sellerUserId.get(), is("seller@seller.de"));
    }
}