package com.ecg.replyts.core.api.model.conversation.command;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

public class ConversationDeletedCommand extends ConversationCommand {
    private final DateTime at;

    public ConversationDeletedCommand(String conversationId, DateTime at) {
        super(conversationId);
        Preconditions.checkNotNull(at);
        Preconditions.checkNotNull(conversationId);
        this.at = at;
    }

    public DateTime getAt() {
        return at;
    }
}