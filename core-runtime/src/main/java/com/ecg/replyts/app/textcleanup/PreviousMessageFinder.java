package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;

import java.util.Optional;

/**
 * Filter a whole conversation, and return previous message given by current message.
 */
class PreviousMessageFinder {

    public Optional<Message> previousMessage(Message currentMessage, Conversation conversation) {
        String inResponseToMessageId = currentMessage.getInResponseToMessageId();
        if (inResponseToMessageId != null) {
            Message message = conversation.getMessageById(inResponseToMessageId);
            if (message != null) return Optional.of(message);
        }

        Message previous = null;
        for (Message message : conversation.getMessages()) {
            if (message.getId().equals(currentMessage.getId())) {
                return Optional.ofNullable(previous);
            }
            previous = message;
        }

        throw new IllegalArgumentException("the given message does not exist in the conversation");
    }
}
