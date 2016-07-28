package com.ecg.messagecenter.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;

class UserNotificationRules {

    boolean buyerShouldBeNotified(Message message) {
        return message.getState() == MessageState.SENT && message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER;
    }

    boolean sellerShouldBeNotified(Message message) {
        return message.getState() == MessageState.SENT && message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER;
    }
}