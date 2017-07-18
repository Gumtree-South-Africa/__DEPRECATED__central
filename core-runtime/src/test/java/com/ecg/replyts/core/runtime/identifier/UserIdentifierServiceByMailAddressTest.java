package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.identifier.UserIdentifierService.DEFAULT_BUYER_USER_ID_NAME;
import static com.ecg.replyts.core.runtime.identifier.UserIdentifierService.DEFAULT_SELLER_USER_ID_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserIdentifierServiceByMailAddressTest {

    private Conversation conversation = mock(Conversation.class);

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
    public void testGetBuyerId() {
        Optional<String> buyerUserId = userIdentifierService.getBuyerUserId(conversation);
        assertThat(buyerUserId.get(), is("buyer@buyer.de"));

        Map<String, String> map = new HashMap<>();
        map.put(DEFAULT_BUYER_USER_ID_NAME,"buyer@buyer.de");
        buyerUserId = userIdentifierService.getBuyerUserId(map);
        assertThat(buyerUserId.get(), is("buyer@buyer.de"));
    }

    @Test
    public void testGetSellerId() {
        Optional<String> sellerUserId = userIdentifierService.getSellerUserId(conversation);
        assertThat(sellerUserId.get(), is("seller@seller.de"));

        Map<String, String> map = new HashMap<>();
        map.put(DEFAULT_SELLER_USER_ID_NAME,"seller@seller.de");
        sellerUserId = userIdentifierService.getSellerUserId(map);
        assertThat(sellerUserId.get(), is("seller@seller.de"));
    }
}
