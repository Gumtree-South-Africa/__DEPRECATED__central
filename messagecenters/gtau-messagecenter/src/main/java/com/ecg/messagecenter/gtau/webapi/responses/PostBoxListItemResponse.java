package com.ecg.messagecenter.gtau.webapi.responses;

import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.messagecenter.gtau.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {

    private final String email;
    private final String id;
    private final String buyerName;
    private final String sellerName;
    private final String buyerId;
    private final String buyerAnonymousEmail;
    private final String sellerId;
    private final String sellerAnonymousEmail;
    private final String adId;
    private final String status;
    private final ConversationRole role;
    private final boolean unread;
    private final List<String> attachments;
    private final MessageResponse lastMessage;

    public PostBoxListItemResponse(String email, ConversationThread conversationThread) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(), "Only supported for data stored as list-aggregate");

        this.email = email;
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ? conversationThread.getBuyerName().get() : "";
        this.sellerName = conversationThread.getSellerName().isPresent() ? conversationThread.getSellerName().get() : "";
        this.buyerId = conversationThread.getBuyerId().orElse("");
        this.buyerAnonymousEmail = conversationThread.getBuyerAnonymousEmail().orElse("");
        this.sellerId = conversationThread.getSellerId().orElse("");
        this.sellerAnonymousEmail = conversationThread.getSellerAnonymousEmail().orElse("");
        this.adId = conversationThread.getAdId();
        this.status = conversationThread.getStatus().orElse("");
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        this.unread = conversationThread.isContainsUnreadMessages();
        this.attachments = conversationThread.getLastMessageAttachments();

        this.lastMessage = new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getReceivedAt()),
                conversationThread.getOfferId(),
                conversationThread.getRobot(),
                ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().get(),
                Optional.empty(),
                MessageResponse.Attachment.transform(
                        conversationThread.getLastMessageAttachments(),
                        Optional.ofNullable(conversationThread.getLastMessageId()).orElse(Optional.empty()).orElse(null)),
                Collections.emptyList(),
                Optional.empty());
    }

    public PostBoxListItemResponse(String email, Conversation conversationRts, ConversationThread conversationThread, MessageResponse messageResponse) {
        this.email = email;
        this.id = conversationRts.getId();
        this.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        this.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        this.buyerId = conversationRts.getBuyerId();
        this.buyerAnonymousEmail = conversationThread.getBuyerAnonymousEmail().orElse(null);
        this.sellerId = conversationRts.getSellerId();
        this.sellerAnonymousEmail = conversationThread.getSellerAnonymousEmail().orElse(null);
        this.adId = conversationRts.getAdId();
        this.status = conversationThread.getStatus().orElse(null);
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        this.unread = conversationThread.isContainsUnreadMessages();
        this.attachments = Collections.emptyList();
        this.lastMessage = messageResponse;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getBuyerAnonymousEmail() {
        return buyerAnonymousEmail;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerAnonymousEmail() {
        return sellerAnonymousEmail;
    }

    public String getAdId() {
        return adId;
    }

    public String getStatus() {
        return status;
    }

    public ConversationRole getRole() {
        return role;
    }

    public boolean isUnread() {
        return unread;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public String getReceivedDate() {
        return lastMessage.getReceivedDate();
    }

    public String getSenderEmail() {
        return lastMessage.getSenderEmail();
    }

    public MailTypeRts getBoundness() {
        return lastMessage.getBoundness();
    }

    public String getTextShortTrimmed() {
        return lastMessage.getTextShortTrimmed();
    }

    public String getRobot() {
        return lastMessage.getRobot();
    }

    public String getOfferId() {
        return lastMessage.getOfferId();
    }
}
