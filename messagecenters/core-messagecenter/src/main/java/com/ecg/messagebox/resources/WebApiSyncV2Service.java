package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;

import java.util.List;
import java.util.Optional;

public interface WebApiSyncV2Service {

    Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, String messageIdCursorOpt, int messagesLimit) throws InterruptedException;

    PostBox archiveConversations(String userId, List<String> conversationIds, int offset, int limit) throws InterruptedException;

}
