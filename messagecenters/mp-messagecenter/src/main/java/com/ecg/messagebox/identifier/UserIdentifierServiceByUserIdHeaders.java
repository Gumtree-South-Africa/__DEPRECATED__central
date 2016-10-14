package com.ecg.messagebox.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.Optional;

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
        return Optional.ofNullable(conversation.getCustomValues().get(customValueName));
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        return Optional.ofNullable(conversation.getCustomValues().get(getBuyerUserIdName()));
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return Optional.ofNullable(conversation.getCustomValues().get(getSellerUserIdName()));
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
