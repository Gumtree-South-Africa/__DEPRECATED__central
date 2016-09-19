package com.ecg.messagebox.service;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.UserUnreadCounts;
import com.ecg.messagebox.model.Visibility;

import java.util.List;
import java.util.Optional;

public interface PostBoxService {

    void processNewMessage(String userId,
                           com.ecg.replyts.core.api.model.conversation.Conversation conversation,
                           com.ecg.replyts.core.api.model.conversation.Message message,
                           boolean isNewReply);

    Optional<ConversationThread> getConversation(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox changeConversationVisibilities(String userId, List<String> conversationIds, Visibility newVis, Visibility returnVis,
                                           int conversationsOffset, int conversationsLimit);

    UserUnreadCounts getUnreadCounts(String userId);

    PostBox blockUser(String userId, String blockedUserId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox unblockUser(String userId, String blockedUserId, Visibility visibility, int conversationsOffset, int conversationsLimit);
}