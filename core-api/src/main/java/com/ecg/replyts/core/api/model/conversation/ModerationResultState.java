package com.ecg.replyts.core.api.model.conversation;

public enum ModerationResultState {
    /**
     * This Message has not yet gotten into any result state.
     */
    UNCHECKED,
    /**
     * Message is considered GOOD and is therefore sent.
     */
    GOOD,
    /**
     * Message is considered BAD and is therefore not delivered.
     */
    BAD,
    /**
     * Message was put into held state by ReplyTS, however it was ignored by CS
     * staff for too long and was sent anyway.
     */
    TIMED_OUT;

    /**
     * Tells if the current state can be picked by a CS Agent during moderation. (only GOOD and BAD allowed)
     */
    public boolean isAcceptableUserDecision() {
        return this == GOOD || this == BAD;
    }

    /**
     * tells if this state is basically an acceptable outcome for any sort of moderation. (also internal moderation due to held timeout)
     */
    public boolean isAcceptableOutcome() {
        return this == TIMED_OUT || isAcceptableUserDecision();
    }

    public boolean allowsSending() {
        return this == GOOD || this == TIMED_OUT;
    }
}
