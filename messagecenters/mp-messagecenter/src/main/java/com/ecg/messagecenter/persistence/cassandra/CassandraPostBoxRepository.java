package com.ecg.messagecenter.persistence.cassandra;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.google.common.base.Optional;

import java.util.List;

/**
 * A postbox repository for Cassandra.
 * <p>
 * In Cassandra the postbox is stored in a row per conversation thread. Unread counters are stored in a separate table.
 * </p><p>
 * The counter needs to be incremented with 1 for new messages with {@link #incrementConversationUnreadMessagesCount(String, String)}.
 * </p><p>
 * Resetting the counter to 0 is done by calling {@link #resetConversationUnreadMessagesCountAsync(String, String)}.
 * </p>
 */
public interface CassandraPostBoxRepository {

    /**
     * Fetches a postbox by it's id.
     *
     * @param postBoxId id of postbox
     * @return a postbox object, even if it's a new postbox
     */
    PostBox getPostBox(String postBoxId);

    /**
     * Fetch a conversation thread.
     *
     * @param postBoxId id of postbox
     * @param conversationId id of conversation
     * @return absent if the thread does not exist, else the conversation thread
     */
    Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId);

    /**
     * @param postBoxId id of postbox
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getUnreadCounts(String postBoxId);

    /**
     * @param postBoxId      id of postbox containing the conversation
     * @param conversationId id of conversation
     * @return the number of unread messages for the given conversation, or 0 when the conversation does not exist
     */
    int getConversationUnreadMessagesCount(String postBoxId, String conversationId);

    /**
     * Increment message unread count for the given postbox/conversation with 1.
     * <p>
     * When the conversation counter does not yet exist, it will be created.
     *
     * @param postBoxId      id of postbox containing the conversation
     * @param conversationId id of conversation
     */
    void incrementConversationUnreadMessagesCount(String postBoxId, String conversationId);

    /**
     * Reset message unread count to 0 for the given postbox/conversation.
     *
     * @param postBoxId      id of postbox containing the conversation
     * @param conversationId id of conversation
     */
    void resetConversationUnreadMessagesCountAsync(String postBoxId, String conversationId);

    /**
     * Add a conversation thread to a postbox, or replace the existing conversation thread with the same id.
     *
     * @param postBoxId          id of postbox that should contain the conversation
     * @param conversationThread the conversation thread to add or replace
     */
    void addReplaceConversationThread(String postBoxId, ConversationThread conversationThread);

    /**
     * Delete conversation threads for a postbox.
     *
     * @param postBoxId       id of postbox containing the conversations
     * @param conversationIds ids of conversations
     */
    void deleteConversationThreadsAsync(String postBoxId, List<String> conversationIds);

    /**
     * Return the list of conversation thread ids belonging to a postbox.
     *
     * @param postBoxId id of postbox
     */
    List<String> getConversationThreadIds(String postBoxId);
}