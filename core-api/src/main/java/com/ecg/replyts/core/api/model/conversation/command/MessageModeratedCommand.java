package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.processing.ModerationAction;
import org.joda.time.DateTime;

/**
 * Command to change the state of a
 * {@link com.ecg.replyts.core.api.model.conversation.Message} in a
 * {@link com.ecg.replyts.core.api.model.conversation.Conversation}
 * based on a human decision.
 */
public class MessageModeratedCommand extends ConversationCommand {
    private final String messageId;
    private final DateTime decidedAt;
    private final ModerationAction moderationAction;

    public MessageModeratedCommand(String conversationId, String messageId, DateTime decidedAt, ModerationAction moderationAction) {
        super(conversationId);
        this.messageId = messageId;
        this.decidedAt = decidedAt;
        this.moderationAction = moderationAction;
    }

    public String getMessageId() {
        return messageId;
    }

    public DateTime getDecidedAt() {
        return decidedAt;
    }

    public ModerationAction getModerationAction() {
        return moderationAction;
    }
}
