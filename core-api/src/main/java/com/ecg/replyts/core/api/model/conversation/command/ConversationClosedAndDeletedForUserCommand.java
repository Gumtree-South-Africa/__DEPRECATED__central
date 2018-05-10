package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

public class ConversationClosedAndDeletedForUserCommand extends ConversationCommand {
    private final DateTime at;
    private String userId;

    public ConversationClosedAndDeletedForUserCommand(String conversationId, String userId, DateTime at) {
        super(conversationId);
        Preconditions.checkNotNull(at);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(userId);
        this.at = at;
        this.userId = userId;
    }

    public DateTime getAt() {
        return at;
    }

    public String getUserId() {
        return userId;
    }
}