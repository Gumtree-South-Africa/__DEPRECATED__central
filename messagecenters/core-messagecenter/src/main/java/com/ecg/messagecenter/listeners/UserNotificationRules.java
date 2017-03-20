package com.ecg.messagecenter.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;

public class UserNotificationRules {
    public boolean buyerShouldBeNotified(Message message) {
        return message.getState() == SENT && message.getMessageDirection() == SELLER_TO_BUYER;
    }

    public boolean sellerShouldBeNotified(Message message) {
        return message.getState() == SENT && message.getMessageDirection() == BUYER_TO_SELLER;
    }
}