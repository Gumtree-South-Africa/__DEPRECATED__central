package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostBoxSingleConversationThreadResponse {

    private String id;
    private ConversationRole role;
    private String buyerEmail;
    private String buyerAnonymousEmail;
    private String sellerEmail;
    private String sellerAnonymousEmail;
    private String buyerName;
    private String sellerName;
    private String adId;
    private List<MessageResponse> messages = new ArrayList<>();
    private long numUnread;
    private boolean blockedByBuyer = false;
    private boolean blockedBySeller = false;

    private PostBoxSingleConversationThreadResponse() {
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(
            long numUnread,
            String email,
            Conversation conversation,
            String buyerAnonymousEmail,
            String sellerAnonymousEmail,
            boolean blockedByBuyer,
            boolean blockedBySeller
    ) {
        PostBoxSingleConversationThreadResponse response = new PostBoxSingleConversationThreadResponse();
        response.id = conversation.getId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversation);
        response.buyerName = conversation.getCustomValues().get("buyer-name") == null ? "Buyer" : conversation.getCustomValues().get("buyer-name");
        response.sellerName = conversation.getCustomValues().get("seller-name") == null ? "Seller" : conversation.getCustomValues().get("seller-name");
        response.adId = conversation.getAdId();
        response.numUnread = numUnread;
        response.buyerEmail = conversation.getBuyerId();
        response.sellerEmail = conversation.getSellerId();
        response.buyerAnonymousEmail = buyerAnonymousEmail;
        response.sellerAnonymousEmail = sellerAnonymousEmail;
        response.blockedByBuyer = blockedByBuyer;
        response.blockedBySeller = blockedBySeller;

        MessagesResponseFactory messagesFactory = new MessagesResponseFactory(new MessagesDiffer());
        List<MessageResponse> builtResponse = messagesFactory.create(email, conversation).collect(Collectors.toList());
        response.messages = builtResponse;

        return builtResponse.isEmpty() ? Optional.empty() : Optional.of(response);
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

    public String getBuyerAnonymousEmail() {
        return buyerAnonymousEmail;
    }

    public String getSellerAnonymousEmail() {
        return sellerAnonymousEmail;
    }

    public boolean isBlockedByBuyer() {
        return blockedByBuyer;
    }

    public boolean isBlockedBySeller() {
        return blockedBySeller;
    }
}
