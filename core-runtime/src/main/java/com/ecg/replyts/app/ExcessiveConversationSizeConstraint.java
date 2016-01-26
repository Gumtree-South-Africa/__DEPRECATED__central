package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;

class ExcessiveConversationSizeConstraint {
    private final int maximumNumberOfMessages;

    public ExcessiveConversationSizeConstraint(int maximumNumberOfMessages) {
        this.maximumNumberOfMessages = maximumNumberOfMessages;
    }

    public boolean tooManyMessagesIn(Conversation conversation) {
        return conversation.getMessages().size() > maximumNumberOfMessages;
    }
}

