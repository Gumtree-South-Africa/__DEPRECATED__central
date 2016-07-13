package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConversationResponse {

    private String id;
    private ConversationRole role;
    private String buyerEmail;
    private String sellerEmail;
    private String buyerName;
    private String sellerName;
    private Long userIdBuyer;
    private Long userIdSeller;
    private String adId;
    private String subject;
    private List<MessageResponse> messages = new ArrayList<>();
    private long numUnread;

    private ConversationResponse() {
    }

    public static Optional<ConversationResponse> create(long numUnread, String userId, Conversation conversationRts, UserIdentifierService userIdentifierService) {
        ConversationResponse response = new ConversationResponse();
        response.id = conversationRts.getId();
        response.role = userIdentifierService.getRoleFromConversation(userId, conversationRts);
        response.buyerName = nullToEmpty(conversationRts.getCustomValues().get("buyer-name"));
        response.sellerName = nullToEmpty(conversationRts.getCustomValues().get("seller-name"));
        response.adId = conversationRts.getAdId();
        response.subject = conversationRts.getMessages().get(0).getHeaders().get("Subject");
        response.numUnread = numUnread;
        response.buyerEmail = conversationRts.getBuyerId();
        response.sellerEmail = conversationRts.getSellerId();
        String buyerUserIdName = userIdentifierService.getBuyerUserIdName();
        response.userIdBuyer = nullSafeParseLong(conversationRts.getCustomValues().get(buyerUserIdName));
        String sellerUserIdName = userIdentifierService.getSellerUserIdName();
        response.userIdSeller = nullSafeParseLong(conversationRts.getCustomValues().get(sellerUserIdName));

        MessagesResponseFactory messagesFactory = new MessagesResponseFactory(userIdentifierService);
        Optional<List<MessageResponse>> builtResponse = messagesFactory.create(userId, conversationRts, conversationRts.getMessages());
        if (builtResponse.isPresent()) {
            response.messages = builtResponse.get();
            return Optional.of(response);
        } else {
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

    public String getSubject() {
        return subject;
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

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Long nullSafeParseLong(String s) {
        return s == null ? null : Long.parseLong(s);
    }
}
