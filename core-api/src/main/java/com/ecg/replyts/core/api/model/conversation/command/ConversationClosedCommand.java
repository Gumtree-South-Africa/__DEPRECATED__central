package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

public class ConversationClosedCommand extends ConversationCommand {
    private final DateTime at;
    private ConversationRole closeIssuer;

    public ConversationClosedCommand(String conversationId, ConversationRole closeIssuer, DateTime at) {
        super(conversationId);
        Preconditions.checkNotNull(at);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(closeIssuer);
        this.at = at;
        this.closeIssuer = closeIssuer;
    }

    public DateTime getAt() {
        return at;
    }

    public ConversationRole getCloseIssuer() {
        return closeIssuer;
    }
}