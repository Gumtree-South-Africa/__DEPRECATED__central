package com.ecg.messagecenter.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;

class UserNotificationRules {

    boolean buyerShouldBeNotified(Message message) {
        return message.getState() == SENT && message.getMessageDirection() == SELLER_TO_BUYER;
    }

    boolean sellerShouldBeNotified(Message message) {
        return message.getState() == SENT && message.getMessageDirection() == BUYER_TO_SELLER;
    }
}