package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserIdentifierServiceByUserIdTest {

    private Conversation conversation = mock(Conversation.class);
    private UserIdentifierService userIdentifierService = new UserIdentifierServiceByUserId();

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
