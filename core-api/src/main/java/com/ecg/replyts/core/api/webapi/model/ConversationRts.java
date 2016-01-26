package com.ecg.replyts.core.api.webapi.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A flow of messages between two persons that are writing about one ad.
 *
 * @author huttar
 * @author Erik van Oosten
 */
public interface ConversationRts {

    /**
     * @return id of conversation
     */
    String getId();

    /**
     * @return user who is the buyer (initiator) in this conversation
     */
    String getBuyer();

    /**
     * @return the anonymous e-mail address of the buyer (initiator) in this conversation
     */
    String getBuyerAnonymousEmail();

    /**
     * @return user who is the seller (respondent) in this conversation
     */
    String getSeller();

    /**
     * @return the anonymous e-mail address of the seller (respondent) in this conversation
     */
    String getSellerAnonymousEmail();

    /**
     * @return conversation's current status
     */
    ConversationRtsStatus getStatus();

    /**
     * @return the platform-dependant id of the ad, this conversation is about
     */
    String getAdId();

    /**
     * @return date when this conversation was created
     */
    Date getCreationDate();

    /**
     * @return date when the conversation was modified the last time
     */
    Date getLastUpdateDate();

    /**
     * @return custom conversation headers (may be provided by platform). These headers do not necessarily need to be
     * available on all webservice calls (depending on the service used)
     * @see #hasConversationHeadersAttached()
     */
    Map<String, String> getConversationHeaders();

    /**
     * a custom conversation header or <code>null</code> if that header does not exist, or conversation headers are not
     * attached
     *
     * @param key conversation header key
     * @return value for that header or <code>null</code>
     * @see #getConversationHeaders()
     * @see #hasConversationHeadersAttached()
     */
    String getConversationHeader(String key);

    /**
     * @return all messages (in chronological order starting with the conversation's first message) that are part of
     * this conversation. Depending on the service called, messages may not be available to you.
     * @see #hasMassagesAttached()
     */
    List<MessageRts> getMessages();

    /**
     * @return tells, if messages are available for this conversation
     * @see #getMessages()
     */
    boolean hasMassagesAttached();

    /**
     * @return tells if conversation headers are available for this conversation
     * @see #getConversationHeaders()
     * @see #getConversationHeader(String)
     */
    boolean hasConversationHeadersAttached();

}
