package com.ecg.messagebox.service;

import com.ecg.messagebox.controllers.requests.PartnerMessagePayload;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;

import java.util.List;
import java.util.Optional;

public interface PostBoxService {

    void processNewMessage(String userId,
                           com.ecg.replyts.core.api.model.conversation.Conversation conversation,
                           com.ecg.replyts.core.api.model.conversation.Message message,
                           boolean isNewReply,
                           String cleanMsgText);

    Optional<ConversationThread> getConversation(String userId, String conversationId, String messageIdCursor, int messagesLimit);

    Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, String messageIdCursorOpt, int messagesLimit);

    PostBox markConversationsAsRead(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox archiveConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit);

    PostBox activateConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit);

    UserUnreadCounts getUnreadCounts(String userId);

    void deleteConversation(String userId, String conversationId, String adId);

    List<String> getConversationsById(String userId, String adId, int resultsLimit);

    Optional<String> storePartnerMessage(PartnerMessagePayload payload);

    void createSystemMessage(String userId, String conversationId, String adId, String text, String customData, boolean sendPush);
}