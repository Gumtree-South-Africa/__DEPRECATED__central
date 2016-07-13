package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostBoxSingleConversationThreadResponse {

    private String id;
    private ConversationRole role;
    private String buyerEmail;
    private String sellerEmail;
    private String buyerName;
    private String sellerName;
    private String adId;
    private List<MessageResponse> messages = new ArrayList<MessageResponse>();
    private long numUnread;

    private PostBoxSingleConversationThreadResponse() {
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts) {
        return create(numUnread, email, conversationRts, true);
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts, boolean robotEnabled) {
        PostBoxSingleConversationThreadResponse response = new PostBoxSingleConversationThreadResponse();
        response.id = conversationRts.getId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.adId = conversationRts.getAdId();
        response.numUnread = numUnread;
        response.buyerEmail = conversationRts.getBuyerId();
        response.sellerEmail = conversationRts.getSellerId();

        MessagesResponseFactory messagesFactory = new MessagesResponseFactory(new MessagesDiffer());
        Optional<List<MessageResponse>> builtResponse = messagesFactory.create(email, conversationRts, conversationRts.getMessages(), robotEnabled);
        if (builtResponse.isPresent()) {
            response.messages = builtResponse.get();
            return Optional.of(response);
        }else {
            return Optional.empty();
        }
    }

    public long getNumUnread() {
        return numUnread;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getId() {
        return id;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getAdId() {
        return adId;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }
}
