package com.ecg.replyts.core.api.model.conversation;

/**
 * Possible states of a {@link Message}.
 */
public enum MessageState {
    /**
     * No filter or human decided on the state yet; the mail is still being processed.
     */
    UNDECIDED,
    /**
     * Message will not be sent out or be displayed.
     */
    DISCARDED,
    /**
     * The mail associated with this message is a DSN or an auto-response.
     */
    IGNORED,
    /**
     * The mail associated with this message was unparsable, and therefore dropped.
     */
    UNPARSABLE,
    /**
     * The message could not be associated with a {@link Conversation}.
     */
    ORPHANED,
    /**
     * The message is held for human review.
     */
    HELD,
    /**
     * The message has been blocked by either a filter or a human.
     */
    BLOCKED,
    /**
     * Message was forwarded to the final recipient.
     */
    SENT;

    /**
     * Tells if this MessageState is a state that cannot be changed anymore.
     * (e.g. SENT messages are sent out and therefore cannot be changed any longer.
     * BLOCKED messages could still be sent out by CS agents)
     */
    public boolean isFinalEndstate() {
        return this == ORPHANED || this == UNPARSABLE || this == SENT || this == IGNORED;
    }

}
