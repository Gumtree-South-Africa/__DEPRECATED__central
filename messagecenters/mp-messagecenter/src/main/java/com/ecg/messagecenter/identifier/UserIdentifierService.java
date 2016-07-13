package com.ecg.messagecenter.identifier;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.Optional;

public interface UserIdentifierService {

    String DEFAULT_SELLER_USER_ID_NAME = "user-id-seller";

    String DEFAULT_BUYER_USER_ID_NAME = "user-id-buyer";

    Optional<String> getUserIdentificationOfConversation(Conversation conversation, ConversationRole role);

    ConversationRole getRoleFromConversation(String id, Conversation conversation);

    ConversationRole getRoleFromConversation(String id, ConversationThread conversation);

    Optional<String> getBuyerUserId(Conversation conversation);

    Optional<String> getSellerUserId(Conversation conversation);

    String getBuyerUserIdName();

    String getSellerUserIdName();
}