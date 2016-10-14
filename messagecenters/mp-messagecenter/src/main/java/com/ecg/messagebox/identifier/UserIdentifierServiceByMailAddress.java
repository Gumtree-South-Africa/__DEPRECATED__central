package com.ecg.messagebox.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.Optional;

public class UserIdentifierServiceByMailAddress implements UserIdentifierService {

    private String buyerUserIdName;

    private String sellerUserIdName;

    public UserIdentifierServiceByMailAddress(String buyerUserIdName, String sellerUserIdName) {
        this.buyerUserIdName = buyerUserIdName;
        this.sellerUserIdName = sellerUserIdName;
    }

    public UserIdentifierServiceByMailAddress() {
        this.buyerUserIdName = DEFAULT_BUYER_USER_ID_NAME;
        this.sellerUserIdName = DEFAULT_SELLER_USER_ID_NAME;
    }

    @Override
    public Optional<String> getUserIdentificationOfConversation(Conversation conversation, ConversationRole role) {
        if (role == ConversationRole.Buyer) {
            return getBuyerUserId(conversation);
        } else if (role == ConversationRole.Seller) {
            return getSellerUserId(conversation);
        } else {
            throw new IllegalArgumentException("Invalid role " + role);
        }
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        return Optional.ofNullable(conversation.getBuyerId());
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return Optional.ofNullable(conversation.getSellerId());
    }

    @Override
    public String getBuyerUserIdName() {
        return buyerUserIdName;
    }

    @Override
    public String getSellerUserIdName() {
        return sellerUserIdName;
    }
}
