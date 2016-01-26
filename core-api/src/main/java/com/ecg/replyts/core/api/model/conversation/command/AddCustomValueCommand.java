package com.ecg.replyts.core.api.model.conversation.command;

public class AddCustomValueCommand extends ConversationCommand{
    private final String conversationId;
    private final String key;
    private final String value;

    public AddCustomValueCommand(String conversationId, String key, String value) {
        super(conversationId);
        this.conversationId = conversationId;
        this.key = key;
        this.value = value;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
