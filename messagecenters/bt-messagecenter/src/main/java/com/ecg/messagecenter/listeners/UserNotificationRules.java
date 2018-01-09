package com.ecg.messagecenter.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;

public final class UserNotificationRules {
    public static boolean buyerShouldBeNotified(Message message) {
        if (message.getState() != MessageState.SENT) {
            return false;
        }

        return message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER;
    }

    public static boolean sellerShouldBeNotified(Message message) {
        if (message.getState() != MessageState.SENT) {
            return false;
        }

        return message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER;
    }
}