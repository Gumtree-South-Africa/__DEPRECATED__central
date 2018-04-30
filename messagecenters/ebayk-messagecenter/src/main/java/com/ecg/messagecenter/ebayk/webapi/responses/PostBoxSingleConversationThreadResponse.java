package com.ecg.messagecenter.ebayk.webapi.responses;

import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.ebayk.util.MessagesDiffer;
import com.ecg.messagecenter.ebayk.util.MessagesResponseFactory;
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
    private Long userIdBuyer;
    private Long userIdSeller;
    private String adId;
    private List<MessageResponse> messages = new ArrayList<>();
    private long numUnread;


    private PostBoxSingleConversationThreadResponse() {
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts) {
        PostBoxSingleConversationThreadResponse response = new PostBoxSingleConversationThreadResponse();
        response.id = conversationRts.getId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.adId = conversationRts.getAdId();
        response.numUnread = numUnread;
        response.buyerEmail = conversationRts.getBuyerId();
        response.sellerEmail = conversationRts.getSellerId();
        response.userIdBuyer = conversationRts.getCustomValues().get("user-id-buyer") == null ? null : Long.parseLong(conversationRts.getCustomValues().get("user-id-buyer"));
        response.userIdSeller = conversationRts.getCustomValues().get("user-id-seller") == null ? null : Long.parseLong(conversationRts.getCustomValues().get("user-id-seller"));


        MessagesResponseFactory messagesFactory = new MessagesResponseFactory(new MessagesDiffer());
        Optional<List<MessageResponse>> builtResponse = messagesFactory.create(email, conversationRts, conversationRts.getMessages());
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

    public Long getUserIdBuyer() {
        return userIdBuyer;
    }

    public Long getUserIdSeller() {
        return userIdSeller;
    }
}
