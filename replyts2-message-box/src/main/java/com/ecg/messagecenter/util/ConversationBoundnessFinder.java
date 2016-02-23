package com.ecg.messagecenter.util;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;

/**
 * User: maldana
 * Date: 26.09.13
 * Time: 14:00
 *
 * @author maldana@ebay.de
 */
public class ConversationBoundnessFinder {

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


}
