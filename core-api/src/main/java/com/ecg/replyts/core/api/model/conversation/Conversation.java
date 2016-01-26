package com.ecg.replyts.core.api.model.conversation;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * A conversation between two {@link com.ecg.replyts.core.api.model.User users}, called buyer
 * (the initiator of the conversation) and the seller, talking about an advertisement (platform specific).
 * Each exchange in the conversation is called a {@link Message}, see {@link #getMessages()}.
 * <p/>
 * Conversations may contain additional custom meta data provided by the platform in the first e-mail
 * see {@link #getCustomValues()}.
 */
public interface Conversation {

    /**
     * @return the unique Id of this conversation
     */
    String getId();

    /**
     * @return the platform specific Id of the ad that this conversation is about
     */
    String getAdId();

    /**
     * @return the user (email address) that is in the buyer role (the person that initiates contact with the seller)
     */
    String getBuyerId();

    /**
     * @return the user (email address) who is in the role of the seller (who has created the ad talked about)
     */
    String getSellerId();

    /**
     * Get user id (email address) for given user.
     *
     * @param role role (not null)
     * @return the user id of either the buyer or seller
     */
    String getUserIdFor(ConversationRole role);

    /**
     * @return the user (email address) that is in the buyer role (the person that initiates contact with the seller)
     */
    String getBuyerSecret();

    /**
     * @return the secret of the seller, e.g. "87a88ucu2cjkkz9"
     */
    String getSellerSecret();

    /**
     * Get secret for given user.
     *
     * @param role role (not null)
     * @return the secret of either the buyer or seller
     */
    String getSecretFor(ConversationRole role);

    /**
     * Either the buyer or the seller, specified by role.
     *
     * @param role the role (not null)
     * @return either buyer or seller (email address)
     */
    String getUserId(ConversationRole role);

    /**
     * @return the date when the first message of this conversation was received by the system.
     */
    DateTime getCreatedAt();

    /**
     * @return the date when the conversation was last modified by the system.
     */
    DateTime getLastModifiedAt();

    /**
     * @return current status of this conversation or null if yet undecided
     */
    ConversationState getState();

    /** @return true if the conversation is CLOSED and the current role did close it. */
    boolean isClosedBy(ConversationRole role);

    /**
     * @return conversation custom values
     */
    Map<String, String> getCustomValues();

    /**
     * @return list of messages belonging to this conversation in chronological order (the first message in the
     * conversation is the first in the result list)
     */
    List<Message> getMessages();

    /**
     * Find message by id.
     *
     * @param messageId a message id
     * @return the message with given id, or null when the message is not present
     */
    Message getMessageById(String messageId);

    /**
     * @return version of this conversation (number of events added to it) - this is not the number of changes of the conversation as a change can consist of multiple events
     */
    int getVersion();

    /**
     * @return true when the conversation was deleted, false otherwise
     */
    boolean isDeleted();
}