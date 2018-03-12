package com.ecg.messagecenter.util;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class ConversationBoundnessFinder {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationBoundnessFinder.class);

    private ConversationBoundnessFinder() {
    }

    public static MailTypeRts boundnessForRole(ConversationRole role, String direction) {
        return boundnessForRole(role, MessageDirection.valueOf(direction));
    }

    public static MailTypeRts boundnessForRole(ConversationRole role, MessageDirection direction) {

        if (role == ConversationRole.Buyer && direction == MessageDirection.BUYER_TO_SELLER)
            return MailTypeRts.OUTBOUND;

        if (role == ConversationRole.Buyer && direction == MessageDirection.SELLER_TO_BUYER)
            return MailTypeRts.INBOUND;

        if (role == ConversationRole.Seller && direction == MessageDirection.SELLER_TO_BUYER)
            return MailTypeRts.OUTBOUND;

        if (role == ConversationRole.Seller && direction == MessageDirection.BUYER_TO_SELLER)
            return MailTypeRts.INBOUND;

        throw new IllegalStateException("Unknown combination");
    }

    public static ConversationRole lookupUsersRole(String email, Conversation conv) {
        return conv.getBuyerId().equalsIgnoreCase(email) ? ConversationRole.Buyer : ConversationRole.Seller;
    }

    public static ConversationRole lookupUsersRole(String email, AbstractConversationThread conv) {
        Optional<String> buyerIdOptional = conv.getBuyerId();
        if (buyerIdOptional.isPresent()) {
            return buyerIdOptional.get().equalsIgnoreCase(email) ? ConversationRole.Buyer : ConversationRole.Seller;
        }

        LOG.warn("No buyer ID for conversation {}", conv.getConversationId());
        return null;
    }
}
