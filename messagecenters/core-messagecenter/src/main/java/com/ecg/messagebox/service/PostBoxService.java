package com.ecg.messagebox.service;

import com.ecg.messagebox.controllers.requests.EmptyConversationRequest;
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

    Optional<ConversationThread> getConversation(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    PostBox markConversationsAsRead(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox changeConversationVisibilities(String userId, List<String> conversationIds, Visibility newVis, Visibility returnVis, int conversationsOffset, int conversationsLimit);

    UserUnreadCounts getUnreadCounts(String userId);

    void deleteConversation(String userId, String conversationId, String adId);

    List<String> resolveConversationIdByUserIdAndAdId(String userId, String adId, int resultsLimit);

    Optional<String> createEmptyConversation(EmptyConversationRequest emptyConversation);

    void createSystemMessage(String userId, String conversationId, String adId, String text, String customData);
}