package com.ecg.replyts.core.api.model.conversation;

/**
 * State of a Conversation.
 */
public enum ConversationState {
    /**
     * The conversation is okay, all messages in this conversation may be delivered.
     */
    ACTIVE(true),

    /**
     * The conversation might be not okay (because one of the users or any message from the conversation) looks like
     * spam. Messages in this conversation may only be delivered after human review
     */
    QUARANTINE(true),

    /**
     * The conversation is blocked for spam reasons. No new message in this conversation may be delivered.
     */
    BLOCKED(false),

    /**
     * The conversation is closed and therefore, no new messages in it will be delivered anymore. This is mostly a
     * spam-protection, to make old e-mail addresses invalid.
     */
    CLOSED(false),

    /**
     * Marks a conversation that just holds a single mail, which is either orphaned or unparsable.
     * Therefore this conversation does not contain any real conversation data.
     */
    DEAD_ON_ARRIVAL(false);

    private final boolean canBeBlocked;

    private ConversationState(boolean canBeBlocked) {
        this.canBeBlocked = canBeBlocked;
    }

    public boolean canBeBlocked() {
        return canBeBlocked;
    }
}
