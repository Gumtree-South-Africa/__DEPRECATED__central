package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private String creationDate;
    private String subject;
    private long numUnread;
    private List<MessageResponse> messages = new ArrayList<>();

    private ConversationResponse() {
    }

    public ConversationResponse(String conversationId, ConversationRole role, String buyerEmail, String sellerEmail, String buyerName,
                                String sellerName, Long userIdBuyer, Long userIdSeller, String adId, String creationDate, String emailSubject,
                                List<MessageResponse> messages, long numUnreadMessages) {
        this.id = conversationId;
        this.role = role;
        this.buyerEmail = buyerEmail;
        this.sellerEmail = sellerEmail;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.userIdBuyer = userIdBuyer;
        this.userIdSeller = userIdSeller;
        this.adId = adId;
        this.creationDate = creationDate;
        this.subject = emailSubject;
        this.numUnread = numUnreadMessages;
        this.messages = messages;
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

    public String getId() {
        return id;
    }

    public long getNumUnread() {
        return numUnread;
    }

    public String getSellerName() {
        return sellerName;
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

    public String getCreationDate() {
        return creationDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationResponse conversationResponse = (ConversationResponse) o;

        return Pairwise.pairsAreEqual(
                id, conversationResponse.id,
                role, conversationResponse.role,
                buyerEmail, conversationResponse.buyerEmail,
                sellerEmail, conversationResponse.sellerEmail,
                buyerName, conversationResponse.buyerName,
                sellerName, conversationResponse.sellerName,
                userIdBuyer, conversationResponse.userIdBuyer,
                userIdSeller, conversationResponse.userIdSeller,
                adId, conversationResponse.adId,
                creationDate, conversationResponse.creationDate,
                subject, conversationResponse.subject,
                numUnread, conversationResponse.numUnread,
                messages, conversationResponse.messages
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, buyerEmail, sellerEmail, buyerName, sellerName, userIdBuyer, userIdSeller,
                adId, creationDate, subject, numUnread, messages);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("role", role)
                .add("buyerEmail", buyerEmail)
                .add("sellerEmail", sellerEmail)
                .add("buyerName", buyerName)
                .add("sellerName", sellerName)
                .add("userIdBuyer", userIdBuyer)
                .add("userIdSeller", userIdSeller)
                .add("adId", adId)
                .add("creationDate", creationDate)
                .add("subject", subject)
                .add("numUnread", numUnread)
                .add("messages", messages)
                .toString();
    }
}
