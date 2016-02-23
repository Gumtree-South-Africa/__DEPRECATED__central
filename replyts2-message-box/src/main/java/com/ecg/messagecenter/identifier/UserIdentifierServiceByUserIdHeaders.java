package com.ecg.messagecenter.identifier;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Optional;

public class UserIdentifierServiceByUserIdHeaders implements UserIdentifierService {

    private final String buyerUserIdName;

    private final String sellerUserIdName;

    public UserIdentifierServiceByUserIdHeaders(String buyerUserIdName, String sellerUserIdName) {
        this.buyerUserIdName = buyerUserIdName;
        this.sellerUserIdName = sellerUserIdName;
    }

    public UserIdentifierServiceByUserIdHeaders() {
        this.buyerUserIdName = DEFAULT_BUYER_USER_ID_NAME;
        this.sellerUserIdName = DEFAULT_SELLER_USER_ID_NAME;
    }

    @Override
    public Optional<String> getUserIdentificationOfConversation(Conversation conversation, ConversationRole role) {
        String customValueName = role == ConversationRole.Buyer ? getBuyerUserIdName() : getSellerUserIdName();
        return Optional.fromNullable(conversation.getCustomValues().get(customValueName));
    }

    @Override
    public ConversationRole getRoleFromConversation(String id, Conversation conversation) {
        Optional<String> buyerId = Optional.fromNullable(conversation.getCustomValues().get(getBuyerUserIdName()));
        Optional<String> sellerId = Optional.fromNullable(conversation.getCustomValues().get(getSellerUserIdName()));


        if (buyerId.isPresent() && buyerId.get().equals(id)) {
            return ConversationRole.Buyer;
        } else if (sellerId.isPresent() && sellerId.get().equals(id)) {
            return ConversationRole.Seller;
        } else {
            throw new IllegalArgumentException("buyer or seller id " + id + " is not found in conversation " + conversation.getId());
        }
    }

    @Override
    public ConversationRole getRoleFromConversation(String id, ConversationThread conversation) {
        Optional<Long> userIdBuyer = conversation.getUserIdBuyer();
        Optional<Long> userIdSeller = conversation.getUserIdSeller();
        if (userIdBuyer.isPresent() && (String.valueOf(userIdBuyer.get()).equals(id))) {
            return ConversationRole.Buyer;
        } else if (userIdSeller.isPresent() && (String.valueOf(userIdSeller.get()).equals(id))) {
            return ConversationRole.Seller;
        }

        Optional<String> buyerId = conversation.getBuyerId();
        if (buyerId.isPresent() && buyerId.get().equals(id)) {
            return ConversationRole.Buyer;
        } else
            return ConversationRole.Seller;
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        return Optional.fromNullable(conversation.getCustomValues().get(getBuyerUserIdName()));
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return Optional.fromNullable(conversation.getCustomValues().get(getSellerUserIdName()));
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