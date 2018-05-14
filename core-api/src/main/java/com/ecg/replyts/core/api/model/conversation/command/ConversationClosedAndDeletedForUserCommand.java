package com.ecg.replyts.core.api.model.conversation.command;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

public class ConversationClosedAndDeletedForUserCommand extends ConversationCommand {
    private final DateTime at;
    private String userEmail;
    private String userId;

    public ConversationClosedAndDeletedForUserCommand(String conversationId, String userId, String userEmail, DateTime at) {
        super(conversationId);
        Preconditions.checkNotNull(at);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(userId);
        this.at = at;
        this.userId = userId;
        this.userEmail = userEmail;
    }

    public DateTime getAt() {
        return at;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }
}