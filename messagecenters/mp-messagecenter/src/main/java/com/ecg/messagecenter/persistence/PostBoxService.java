package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;

import java.util.List;
import java.util.Optional;

public interface PostBoxService {

    void processNewMessage(String userId, Conversation conversation, Message message, ConversationRole role, boolean newReplyArrived);

    Optional<ConversationResponse> getConversation(String userId, String conversationId);

    Optional<ConversationResponse> markConversationAsRead(String userId, String conversationId);

    PostBoxResponse getConversations(String userId, Integer size, Integer page);

    /**
     * Delete conversations for a postbox and return the new postbox.
     * <p>
     * For performance optimizations, when a client is not interested in the response,
     * passing a size of 0, will not return a postbox response.
     * </p><p>
     * We might not read our own writes (i.e. the just deleted conversation threads are not replicated yet),
     * so after loading the new postbox, we will remove the just deleted conversation threads to make sure we don't
     * return deleted conversation back to the client.
     * </p>
     *
     * @param userId          user id
     * @param conversationIds ids of conversations
     * @param size            size
     * @param page            page
     * @return the new postbox or null if size is 0
     */
    PostBoxResponse deleteConversations(String userId, List<String> conversationIds, Integer size, Integer page);

    /**
     * @param userId user id
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getUnreadCounts(String userId);

    /**
     * Return the list with response data per conversation for the user.
     *
     * @param userId user id
     * @return the list with response data
     */
    List<ResponseData> getResponseData(String userId);
}