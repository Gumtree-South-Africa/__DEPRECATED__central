package com.ecg.messagebox.service;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.PostBoxUnreadCounts;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagecenter.persistence.NewMessageListener;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.List;
import java.util.Optional;

public interface PostBoxService {

    void processNewMessage(String postBoxId,
                           com.ecg.replyts.core.api.model.conversation.Conversation conversation,
                           com.ecg.replyts.core.api.model.conversation.Message message,
                           ConversationRole conversationRole,
                           boolean newReplyArrived,
                           Optional<NewMessageListener> postBoxWriteCallback);

    Optional<ConversationThread> getConversation(String postBoxId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    Optional<ConversationThread> markConversationAsRead(String postBoxId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit);

    PostBox markConversationsAsRead(String postBoxId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox getConversations(String postBoxId, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBox changeConversationVisibilities(String postBoxId, List<String> conversationIds, Visibility visibility, int conversationsOffset, int conversationsLimit);

    PostBoxUnreadCounts getUnreadCounts(String postBoxId);
}