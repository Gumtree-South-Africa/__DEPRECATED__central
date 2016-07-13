package com.ecg.messagebox.persistence;

import com.ecg.messagebox.model.BlockedUserInfo;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.PostBoxUnreadCounts;
import com.ecg.messagebox.model.Visibility;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * @param postBoxId id of postbox
     * @return a postbox object, even if it's a new postbox
     */
    PostBox getPostBox(String postBoxId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    /**
     * Fetches a conversation.
     * <p>
     * Does not retrieve the unread messages count or any of the conversation's messages.
     * For that use the {@link #getConversationWithMessages(String, String, Optional, int)} method.
     * </p>
     *
     * @param postBoxId      user identifier
     * @param conversationId id of conversation
     * @return absent if the conversation does not exist, else returns the conversation
     */
    Optional<ConversationThread> getConversation(String postBoxId, String conversationId);

    /**
     * Fetches a conversation with its unread messages count and a cursor-based paginated list of its messages.
     *
     * @param postBoxId          id of postbox
     * @param conversationId     id of conversation
     * @param messageIdCursorOpt message id cursor optional, used in cursor-based pagination of messages
     * @param messagesLimit      maximum number of returned messages
     * @return absent if the conversation does not exist, else returns the conversation, the unread messages count and a paginated list of its messages
     */
    Optional<ConversationThread> getConversationWithMessages(String postBoxId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    /**
     * @param postBoxId id of postbox
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getPostBoxUnreadCounts(String postBoxId);

    /**
     * @param postBoxId      id of postbox containing the conversation
     * @param conversationId id of conversation
     * @return the number of unread messages for the given conversation, or 0 when the conversation does not exist
     */
    int getConversationUnreadCount(String postBoxId, String conversationId);

    /**
     * Resets the message unread count to 0 for the given user/conversation/ad.
     *
     * @param postBoxId      id of postbox containing the conversation
     * @param conversationId id of conversation
     * @param adId           id of ad
     */
    void resetConversationUnreadCount(String postBoxId, String conversationId, String adId);

    /**
     * Creates a new conversation with the passed message.
     *
     * @param postBoxId            identifier of user that this conversation belongs to
     * @param conversation         the conversation to add or replace
     * @param message              the new message to be added
     * @param incrementUnreadCount if true, the conversation's unread count will be incremented, otherwise not
     */
    void createConversation(String postBoxId, ConversationThread conversation, Message message, boolean incrementUnreadCount);

    /**
     * Adds a new message to an existing conversation.
     *
     * @param postBoxId            identifier of user that this conversation belongs to
     * @param conversationId       id of the conversation to add the message to
     * @param message              the new message to be added
     * @param incrementUnreadCount if true, the conversation's unread count will be incremented, otherwise not
     */
    void addMessage(String postBoxId, String conversationId, String adId, Message message, boolean incrementUnreadCount);

    /**
     * Change conversation visibilities for a postbox.
     *
     * @param postBoxId            id of postbox containing the conversations
     * @param adConversationIdsMap map of ad id to conversation id
     * @param visibility           the new visibility
     */
    void changeConversationVisibilities(String postBoxId, Map<String, String> adConversationIdsMap, Visibility visibility);

    void deleteConversations(String postBoxId, Map<String, String> adConversationIdsMap);

    void deleteConversation(String postBoxId, String adId, String conversationId);

    Map<String, String> getAdConversationIdsMap(String postBoxId, List<String> conversationIds);

    /**
     * Return the list of conversation ids belonging to a postbox.
     *
     * @param postBoxId id of postbox
     */
    List<String> getConversationIds(String postBoxId);

    void blockUser(String reporterUserId, String blockedUserId);

    void unblockUser(String reporterUserId, String blockedUserId);

    Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId12);

    boolean areUsersBlocked(String userId1, String userId2);
}