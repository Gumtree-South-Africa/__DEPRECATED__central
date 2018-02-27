package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class UserIdentifierServiceByUserIdHeaders implements UserIdentifierService {
    private static final Logger LOG = LoggerFactory.getLogger(UserIdentifierServiceByUserIdHeaders.class);

    private final String buyerUserIdName;

    private final String sellerUserIdName;

    public UserIdentifierServiceByUserIdHeaders(String buyerUserIdName, String sellerUserIdName) {
        this.buyerUserIdName = buyerUserIdName;
        this.sellerUserIdName = sellerUserIdName;
        LOG.info("buyerUserIdName is [{}] sellerUserIdName is [{}]", this.buyerUserIdName, this.sellerUserIdName);
    }

    public UserIdentifierServiceByUserIdHeaders() {
        this(DEFAULT_BUYER_USER_ID_NAME, DEFAULT_SELLER_USER_ID_NAME);
    }

    @Override
    public Optional<String> getUserIdentificationOfConversation(Conversation conversation, ConversationRole role) {
        String customValueName = role == ConversationRole.Buyer ? getBuyerUserIdName() : getSellerUserIdName();
        return Optional.ofNullable(conversation.getCustomValues().get(customValueName));
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        return getBuyerUserId(conversation.getCustomValues());
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        return getSellerUserId(conversation.getCustomValues());
    }

    @Override
    public Optional<String> getBuyerUserId(Map<String, String> mailHeaders) {
        return Optional.ofNullable(mailHeaders.get(getBuyerUserIdName()));
    }

    @Override
    public Optional<String> getSellerUserId(Map<String, String> mailHeaders) {
        return Optional.ofNullable(mailHeaders.get(getSellerUserIdName()));
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
