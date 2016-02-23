package com.ecg.messagecenter.identifier;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Optional;

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
        if(role == ConversationRole.Buyer) {
            return getBuyerUserId(conversation);
        } else if(role == ConversationRole.Seller) {
            return getSellerUserId(conversation);
        } else {
            throw new IllegalArgumentException("Invalid role " + role);
        }
    }

    @Override
    public ConversationRole getRoleFromConversation(String id, Conversation conversation) {

        if(conversation.getBuyerId().toLowerCase().equals(id.toLowerCase())) {
            return ConversationRole.Buyer;
        } else if(conversation.getSellerId().toLowerCase().equals(id.toLowerCase())) {
            return ConversationRole.Seller;
        } else {
            throw new IllegalArgumentException("buyer or seller id " + id  + " is not found in conversation " + conversation.getId());
        }
    }

    @Override
    public ConversationRole getRoleFromConversation(String id, ConversationThread conversation) {
        return conversation.getBuyerId().get().toLowerCase().equals(id.toLowerCase()) ? ConversationRole.Buyer : ConversationRole.Seller;
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        return Optional.fromNullable(conversation.getBuyerId());
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return Optional.fromNullable(conversation.getSellerId());
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