package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;

import java.util.List;
import java.util.Optional;

public interface PostBoxService {

    void processNewMessage(String postBoxId,
                           Conversation conversation,
                           Message message,
                           ConversationRole conversationRole,
                           boolean newReplyArrived);

    Optional<ConversationResponse> getConversation(String postBoxId, String conversationId);

    Optional<ConversationResponse> markConversationAsRead(String postBoxId, String conversationId);

    PostBoxResponse getConversations(String postBoxId, Integer size, Integer page);

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
     * @param postBoxId       id of postbox containing the conversations
     * @param conversationIds ids of conversations
     * @param size            size
     * @param page            page
     * @return the new postbox or null if size is 0
     */
    PostBoxResponse deleteConversations(String postBoxId, List<String> conversationIds, Integer size, Integer page);

    /**
     * @param postBoxId id of postbox
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getUnreadCounts(String postBoxId);

    /**
     * Return the list with response data per conversation for the user.
     *
     * @param userId the user id
     * @return the list with response data
     */
    List<ResponseData> getResponseData(String userId);
}
