package com.ecg.messagecenter.core.listeners;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;

public class UserNotificationRules {
    public boolean buyerShouldBeNotified(MessageState state, MessageDirection messageDirection) {
        return state == SENT && messageDirection == SELLER_TO_BUYER;
    }

    public boolean sellerShouldBeNotified(MessageState state, MessageDirection messageDirection) {
        return state == SENT && messageDirection == BUYER_TO_SELLER;
    }
}