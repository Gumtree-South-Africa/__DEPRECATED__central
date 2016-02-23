package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;

public interface PostBoxService {

    void processNewMessage(String userId,
                           Conversation conversation,
                           Message message,
                           ConversationRole conversationRole,
                           boolean newReplyArrived,
                           Optional<PushMessageOnUnreadConversationCallback> postBoxWriteCallback);

    Optional<ConversationResponse> getConversation(String userId, String conversationId);

    Optional<ConversationResponse> updateConversationToRead(String userId, String conversationId);
}