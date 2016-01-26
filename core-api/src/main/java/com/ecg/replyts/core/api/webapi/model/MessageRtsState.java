package com.ecg.replyts.core.api.webapi.model;

/**
 * State of a {@link MessageRts message}.
 *
 * @author huttar
 */
public enum MessageRtsState {

    /**
     * The mail associated with this message is a DSN or an autoresponse.
     */
    IGNORED,
    /**
     * Message will not be sent out or be displayed.
     */
    DISCARDED,
    /**
     * message could not be parsed
     */
    UNPARSEABLE,
    /**
     * The message could not be associated with a Conversation.
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
    SENT,

    /**
     * Message is in an system-internal state that is not publicly available or your current client version does not
     * support this state.
     */
    OTHER;


}
