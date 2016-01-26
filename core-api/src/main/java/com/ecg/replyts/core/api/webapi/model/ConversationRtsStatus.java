package com.ecg.replyts.core.api.webapi.model;

/**
 * Status of a {@link ConversationRts conversation}.
 *
 * @author huttar
 */
public enum ConversationRtsStatus {
    /**
     * The Conversation is okay, all messages belonging to this conversation will be delivered.
     */
    ACTIVE,
    /**
     * The conversation holds a single message that was not able to be processed. it contains the minimum amount of data possible.
     */
    DEAD_ON_ARRIVAL,

    /** conversation is closed - new messages will be omitted. */
    CLOSED,
    /**
     * Indicates that the conversation is in any other internal state that is not public available.
     */
    OTHER;

}
