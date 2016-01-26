package com.ecg.replyts.core.api.model.conversation.command;

/**
 * Base class for all conversation commands.
 */
public abstract class ConversationCommand {
    private final String conversationId;

    protected ConversationCommand(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) throw new IllegalArgumentException();
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
