package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * UK specific User identifier services
 * - Delete this class after a migration
 * <p>
 * Reason:
 * - Old anonymous messages (conversations that contain SELLER_ID but does not contain BUYER_ID, use email in this case)
 */
public class UkUserIdentifierServiceByUserIdHeaders extends UserIdentifierServiceByUserId {
    private static final Logger LOG = LoggerFactory.getLogger(UkUserIdentifierServiceByUserIdHeaders.class);

    public UkUserIdentifierServiceByUserIdHeaders(String buyerUserIdName, String sellerUserIdName) {
        super(buyerUserIdName, sellerUserIdName);
        LOG.info("MSGBOX MIGRATION: IdentifierService: {}", UkUserIdentifierServiceByUserIdHeaders.class.getSimpleName());
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        Optional<String> userId = getBuyerUserId(conversation.getCustomValues());
        if (!userId.isPresent()) {
            LOG.info("MSGBOX MIGRATION: Buyer ID is not available in headers");

            userId = Optional.ofNullable(conversation.getBuyerId());
            if (!userId.isPresent()) {
                LOG.info("MSGBOX MIGRATION: Buyer ID is not available in BUYER_ID field");
                return Optional.empty();
            }
        }

        return userId;
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        Optional<String> userId = getSellerUserId(conversation.getCustomValues());
        if (!userId.isPresent()) {
            LOG.info("MSGBOX MIGRATION: Seller ID is not available in headers");
        }

        return userId;
    }
}
