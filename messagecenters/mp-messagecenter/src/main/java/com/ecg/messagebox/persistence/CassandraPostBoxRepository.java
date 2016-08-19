package com.ecg.messagebox.persistence;

import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.model.*;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The user's conversations repository for Cassandra.
 * <p>
 * The counter needs to be incremented with 1 for new messages.
 * </p><p>
 * Resetting the counter to 0 is done by calling {@link #resetConversationUnreadCount(String, String, String)}.
 * </p>
 */
public interface CassandraPostBoxRepository {

    /**
     * Returns a paginated list of conversations for the specified postbox id and visibility.
     *
     * @param userId id of postbox
     * @return a postbox object, even if it's a new postbox
     */
    PostBox getPostBox(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    /**
     * Fetches a conversation.
     * <p>
     * Does not retrieve the unread messages count or any of the conversation's messages.
     * For that use the {@link #getConversationWithMessages(String, String, Optional, int)} method.
     * </p>
     *
     * @param userId      user identifier
     * @param conversationId id of conversation
     * @return absent if the conversation does not exist, else returns the conversation
     */
    Optional<ConversationThread> getConversation(String userId, String conversationId);

    /**
     * Fetches a conversation with its unread messages count and a cursor-based paginated list of its messages.
     *
     * @param userId          id of postbox
     * @param conversationId     id of conversation
     * @param messageIdCursorOpt message id cursor optional, used in cursor-based pagination of messages
     * @param messagesLimit      maximum number of returned messages
     * @return absent if the conversation does not exist, else returns the conversation, the unread messages count and a paginated list of its messages
     */
    Optional<ConversationThread> getConversationWithMessages(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    /**
     * @param userId id of postbox
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getPostBoxUnreadCounts(String userId);

    /**
     * @param userId      id of postbox containing the conversation
     * @param conversationId id of conversation
     * @return the number of unread messages for the given conversation, or 0 when the conversation does not exist
     */
    int getConversationUnreadCount(String userId, String conversationId);

    /**
     * Resets the message unread count to 0 for the given user/conversation/ad.
     *
     * @param userId      id of postbox containing the conversation
     * @param conversationId id of conversation
     * @param adId           id of ad
     */
    void resetConversationUnreadCount(String userId, String conversationId, String adId);

    /**
     * Creates a new conversation with the passed message.
     *
     * @param userId            identifier of user that this conversation belongs to
     * @param conversation         the conversation to add or replace
     * @param message              the new message to be added
     * @param incrementUnreadCount if true, the conversation's unread count will be incremented, otherwise not
     */
    void createConversation(String userId, ConversationThread conversation, Message message, boolean incrementUnreadCount);

    /**
     * Adds a new message to an existing conversation.
     *
     * @param userId            identifier of user that this conversation belongs to
     * @param conversationId       id of the conversation to add the message to
     * @param message              the new message to be added
     * @param incrementUnreadCount if true, the conversation's unread count will be incremented, otherwise not
     */
    void addMessage(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount);

    /**
     * Change conversation visibilities for a postbox.
     *
     * @param userId            id of postbox containing the conversations
     * @param adConversationIdsMap map of ad id to conversation id
     * @param visibility           the new visibility
     */
    void changeConversationVisibilities(String userId, Map<String, String> adConversationIdsMap, Visibility visibility);

    void deleteConversations(String userId, Map<String, String> adConversationIdsMap);

    void deleteConversation(String userId, String adId, String conversationId);

    Map<String, String> getConversationAdIdsMap(String userId, List<String> conversationIds);

    Stream<ConversationModification> getConversationModificationsByHour(DateTime date);
    
    void deleteModificationIndexByDate(DateTime modifiedDate, UUID messageId, String userId, String conversationId);

    void blockUser(String reporterUserId, String blockedUserId);

    void unblockUser(String reporterUserId, String blockedUserId);

    Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId12);

    boolean areUsersBlocked(String userId1, String userId2);

    ConversationModification getLastConversationModification(String userId, String convId);

    DateTime getCronjobLastProcessedDate(String jobName);

    void setCronjobLastProcessedDate(String jobName, DateTime lastProcessed);
}