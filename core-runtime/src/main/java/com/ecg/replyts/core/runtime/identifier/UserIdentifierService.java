package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.Map;
import java.util.Optional;

public interface UserIdentifierService {
    String DEFAULT_SELLER_USER_ID_NAME = "user-id-seller";
    String DEFAULT_BUYER_USER_ID_NAME = "user-id-buyer";

    Optional<String> getUserIdentificationOfConversation(Conversation conversation, ConversationRole role);

    Optional<String> getBuyerUserId(Conversation conversation);

    Optional<String> getSellerUserId(Conversation conversation);

    Optional<String> getBuyerUserId(Map<String, String> mailHeaders);

    Optional<String> getSellerUserId(Map<String, String> mailHeaders);

    String getBuyerUserIdName();

    String getSellerUserIdName();
}