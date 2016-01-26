package com.ecg.replyts.core.api.webapi.model;

/**
 * {@link MessageRts Message} direction within a {@link ConversationRts converation}.
 *
 * @author huttar
 * @author Erik van Oosten
 */
public enum MessageRtsDirection {

    /**
     * Message is sent by the buyer (initiator).
     */
    BUYER_TO_SELLER,
    /**
     * Message is sent by the seller (respondent).
     */
    SELLER_TO_BUYER,
    /**
     * unparsable messages or orphaned messages cannot be assigned a  message direction. therefore, their direction is unknown.
     */
    UNKNOWN;

}
