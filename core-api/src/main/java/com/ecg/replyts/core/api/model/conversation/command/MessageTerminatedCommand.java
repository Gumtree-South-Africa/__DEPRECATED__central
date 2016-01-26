package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Command indicating that a preprocessor has decided that a message should not be sent. Reason for this decision is attached.
 */
public class MessageTerminatedCommand extends ConversationCommand {

    private final Class<?> issuer;

    private final String reason;

    private final String messageId;

    private final MessageState terminationState;

    private static final Set<MessageState> ACCEPTABLE_TERMINATION_STATES = ImmutableSet.of(
            MessageState.DISCARDED,
            MessageState.IGNORED,
            MessageState.BLOCKED,
            MessageState.UNPARSABLE,
            MessageState.ORPHANED,
            MessageState.HELD,
            MessageState.SENT);

    /**
     * @param conversationId   id of conversation, the message belongs to
     * @param messageId        the message's id of concern
     * @param issuer           type of preprocessor/filter who made the decision
     * @param reason           human readable description for the decision
     * @param terminationState state the message should end up. Only accepts states that are in ACCEPTABLE_TERMINATION_STATES.
     */
    public MessageTerminatedCommand(String conversationId, String messageId, Class<?> issuer, String reason, MessageState terminationState) {
        super(conversationId);
        if (!ACCEPTABLE_TERMINATION_STATES.contains(terminationState)) {
            throw new IllegalArgumentException("Termination state must be in " + ACCEPTABLE_TERMINATION_STATES);
        }
        this.messageId = messageId;
        this.terminationState = terminationState;
        this.reason = reason;
        this.issuer = issuer;
    }


    public String getMessageId() {
        return messageId;
    }

    public Class<?> getIssuer() {
        return issuer;
    }

    public String getReason() {
        return reason;
    }

    public MessageState getTerminationState() {
        return terminationState;
    }
}
