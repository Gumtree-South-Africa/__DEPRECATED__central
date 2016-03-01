package com.ecg.de.ebayk.messagecenter.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;

class UserNotificationRules {

    public boolean buyerShouldBeNotified(Message message) {
        if (message.getState() != MessageState.SENT) {
            return false;
        }

        return message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER;
    }

    public boolean sellerShouldBeNotified(Message message) {
        if (message.getState() != MessageState.SENT) {
            return false;
        }

        return message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER;
    }

}
