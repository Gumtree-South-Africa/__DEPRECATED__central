package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Migration and synchronization purposes between MessageCenter and MessageBox.
 * - If the conversation was created without user-ids but the user-ids was provided afterwards, then pick up those RAW headers
 * from messages (because user-ids are put into custom-headers)
 */
public class CaUserIdentifierServiceByUserIdHeaders extends UserIdentifierServiceByUserId {
    private static final Logger LOG = LoggerFactory.getLogger(CaUserIdentifierServiceByUserIdHeaders.class);

    private static final String RAW_BUYER_HEADER = "X-Cust-User-Id-Buyer";
    private static final String RAW_SELLER_HEADER = "X-Cust-User-Id-Seller";

    public CaUserIdentifierServiceByUserIdHeaders(String buyerUserIdName, String sellerUserIdName) {
        super(buyerUserIdName, sellerUserIdName);
        LOG.info("MSGBOX MIGRATION: IdentifierService: {}", CaUserIdentifierServiceByUserIdHeaders.class.getSimpleName());
    }

    @Override
    public Optional<String> getBuyerUserId(Conversation conversation) {
        Optional<String> userId = getBuyerUserId(conversation.getCustomValues());
        userId = userId.isPresent() ? userId : getRawHeader(conversation, RAW_BUYER_HEADER);

        // Use email as the last option
        // Reason: We want to keep running annonymous messages where Seller-ID is known and seller uses MessageBox
        // which means we know his user-id (in conversation events). Therefore, we need to have even an user-id for a buyer
        // otherwise new message in the anonymous conversation is going to be ignored by MessageBox and even seller does not get
        // the message into the V2.
        if (!userId.isPresent()) {
            LOG.info("User ID not found, fallback to an E-mail: " + conversation.getBuyerId() + ", Conversation-ID: " + conversation.getId());
            userId = Optional.ofNullable(conversation.getBuyerId());
        }

        return userId;
    }

    @Override
    public Optional<String> getSellerUserId(Conversation conversation) {
        Optional<String> userId = getSellerUserId(conversation.getCustomValues());
        return userId.isPresent() ? userId : getRawHeader(conversation, RAW_SELLER_HEADER);
    }

    private static Optional<String> getRawHeader(Conversation conversation, String headerName) {
        String userId = null;
        for (Message message: conversation.getMessages()) {
            userId = message.getHeaders().get(headerName);

            if (userId != null) {
                return Optional.of(userId);
            }
        }

        return Optional.empty();
    }
}
