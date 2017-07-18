package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.Map;
import java.util.Optional;

public class UserIdentifierServiceByMailAddress implements UserIdentifierService {

    private final String buyerUserIdName;

    private final String sellerUserIdName;

    public UserIdentifierServiceByMailAddress(String buyerUserIdName, String sellerUserIdName) {
        this.buyerUserIdName = buyerUserIdName;
        this.sellerUserIdName = sellerUserIdName;
    }

    public UserIdentifierServiceByMailAddress() {
        this(DEFAULT_BUYER_USER_ID_NAME, DEFAULT_SELLER_USER_ID_NAME);
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
    public Optional<String> getBuyerUserId(Map<String, String> mailHeaders) {
        return Optional.ofNullable(mailHeaders.get(buyerUserIdName));
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return Optional.ofNullable(conversation.getSellerId());
    }

    @Override
    public Optional<String> getSellerUserId(Map<String, String> mailHeaders) {
        return Optional.ofNullable(mailHeaders.get(sellerUserIdName));
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
