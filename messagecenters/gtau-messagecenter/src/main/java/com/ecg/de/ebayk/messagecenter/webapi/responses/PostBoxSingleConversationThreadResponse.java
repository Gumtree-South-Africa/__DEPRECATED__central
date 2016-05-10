package com.ecg.de.ebayk.messagecenter.webapi.responses;

import com.ecg.de.ebayk.messagecenter.util.*;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

/**
* User: maldana
* Date: 30.10.13
* Time: 17:14
*
* @author maldana@ebay.de
*/
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
    private String negotiationId;


    private PostBoxSingleConversationThreadResponse() {
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts) {
        return create(numUnread, email, conversationRts, true);
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts, boolean robotEnabled) {
        PostBoxSingleConversationThreadResponse response = new PostBoxSingleConversationThreadResponse();
        response.id = conversationRts.getId();
        response.negotiationId = conversationRts.getCustomValues().get("negotiationid");
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
            return Optional.absent();
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

    public Long getNegotiationId() {
        return negotiationId == null ? null : Long.valueOf(negotiationId);
    }
}
