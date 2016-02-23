package com.ecg.messagecenter.identifier;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserIdentifierServiceByUserIdHeadersTest {

    private Conversation conversation = mock(Conversation.class);

    private ConversationThread conversationThread = mock(ConversationThread.class);

    private UserIdentifierService userIdentifierService = new UserIdentifierServiceByUserIdHeaders();

    @Test
    public void testGetBuyerUserIdFromHeaders() throws Exception {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_BUYER_USER_ID_NAME, "id4711");
        when(conversation.getCustomValues()).thenReturn(customValues);

        String userId = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Buyer).get();
        assertThat(userId, is("id4711"));
    }

    @Test
    public void testGetSellerUserIdFromHeaders() throws Exception {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_SELLER_USER_ID_NAME, "id4711");
        when(conversation.getCustomValues()).thenReturn(customValues);

        String userId = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Seller).get();
        assertThat(userId, is("id4711"));
    }


    @Test
    public void testGetSellerUserIdNotExisting() throws Exception {
        HashMap<String, String> customValues = Maps.newHashMap();
        when(conversation.getCustomValues()).thenReturn(customValues);

        Optional<String> userId = userIdentifierService.getUserIdentificationOfConversation(conversation, ConversationRole.Seller);
        assertThat(userId.isPresent(), is(false));
    }

    @Test
    public void testSellerRole() {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_SELLER_USER_ID_NAME, "id4711");
        when(conversation.getCustomValues()).thenReturn(customValues);
        ConversationRole role = userIdentifierService.getRoleFromConversation("id4711", conversation);
        assertThat(role, is(ConversationRole.Seller));
    }

    @Test
    public void testBuyerRole() {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_BUYER_USER_ID_NAME, "id4711");
        when(conversation.getCustomValues()).thenReturn(customValues);
        ConversationRole role = userIdentifierService.getRoleFromConversation("id4711", conversation);
        assertThat(role, is(ConversationRole.Buyer));
    }

    @Test
    public void testGetRoleForConversationGivenUserIdWhenEqualsUserIdBuyerThenReturnsRoleBuyer() {
        when(conversationThread.getUserIdBuyer()).thenReturn(com.google.common.base.Optional.of(123L));
        assertThat(userIdentifierService.getRoleFromConversation("123", conversationThread), is(ConversationRole.Buyer));
    }

    @Test
    public void testGetRoleForConversationGivenUserIdWhenEqualsBuyerIdThenReturnsRoleBuyer() {
        when(conversationThread.getUserIdBuyer()).thenReturn(com.google.common.base.Optional.of(0L));
        when(conversationThread.getUserIdSeller()).thenReturn(com.google.common.base.Optional.of(0L));
        when(conversationThread.getBuyerId()).thenReturn(com.google.common.base.Optional.of("123"));
        assertThat(userIdentifierService.getRoleFromConversation("123", conversationThread), is(ConversationRole.Buyer));
    }


    @Test
    public void testGetRoleForConversationGivenUserIdWhenEqualsUserIdSellerThenReturnsRoleSeller() {
        when(conversationThread.getUserIdBuyer()).thenReturn(com.google.common.base.Optional.of(0L));
        when(conversationThread.getUserIdSeller()).thenReturn(com.google.common.base.Optional.of(123L));
        assertThat(userIdentifierService.getRoleFromConversation("123", conversationThread), is(ConversationRole.Seller));
    }

    @Test
    public void testGetRoleForConversationGivenUserIdWhenEqualsSellerIdThenReturnsRoleSeller() {
        when(conversationThread.getUserIdBuyer()).thenReturn(com.google.common.base.Optional.of(0L));
        when(conversationThread.getUserIdSeller()).thenReturn(com.google.common.base.Optional.of(0L));
        when(conversationThread.getBuyerId()).thenReturn(com.google.common.base.Optional.of("000"));
        assertThat(userIdentifierService.getRoleFromConversation("123", conversationThread), is(ConversationRole.Seller));
    }

    @Test
    public void testGetBuyerId() {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_BUYER_USER_ID_NAME, "id4711");
        when(conversation.getCustomValues()).thenReturn(customValues);
        Optional<String> buyerUserId = userIdentifierService.getBuyerUserId(conversation);
        assertThat(buyerUserId.get(), is("id4711"));
    }

    @Test
    public void testGetSellerId() {
        HashMap<String, String> customValues = Maps.newHashMap();
        customValues.put(UserIdentifierService.DEFAULT_SELLER_USER_ID_NAME, "id4712");
        when(conversation.getCustomValues()).thenReturn(customValues);
        Optional<String> sellerUserId = userIdentifierService.getSellerUserId(conversation);
        assertThat(sellerUserId.get(), is("id4712"));
    }
}