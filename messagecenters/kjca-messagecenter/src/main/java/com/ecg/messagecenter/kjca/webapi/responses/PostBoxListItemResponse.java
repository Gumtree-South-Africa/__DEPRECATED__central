package com.ecg.messagecenter.kjca.webapi.responses;

import com.ecg.messagecenter.kjca.persistence.block.ConversationBlock;
import com.ecg.messagecenter.kjca.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {

    private String email;
    private String id;
    private String buyerName;
    private String buyerEmail;
    private String sellerName;
    private String adId;

    private ConversationRole role;
    private boolean unread;
    private List<String> attachments = Collections.emptyList();

    private MessageResponse lastMessage;

    private boolean blockedByBuyer = false;
    private boolean blockedBySeller = false;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(
            String email,
            ConversationThread conversationThread,
            ConversationBlock conversationBlock
    ) {
        this.email = email;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().orElse("Buyer");
        this.sellerName = conversationThread.getSellerName().orElse("Seller");
        this.buyerEmail = conversationThread.getBuyerId().get();
        this.adId = conversationThread.getAdId();
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        this.blockedByBuyer = conversationBlock != null && conversationBlock.getBuyerBlockedSellerAt().isPresent();
        this.blockedBySeller = conversationBlock != null && conversationBlock.getSellerBlockedBuyerAt().isPresent();

        this.lastMessage = new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getReceivedAt()),
                ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().orElse(""),
                Optional.empty(),
                Collections.<MessageResponse.Attachment>emptyList(),
                conversationThread.getBuyerId().get());
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

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getAdId() {
        return adId;
    }

    public String getReceivedDate() {
        return lastMessage.getReceivedDate();
    }

    public String getSenderEmail() {
        return lastMessage.getSenderEmail();
    }

    public ConversationRole getRole() {
        return role;
    }

    public MailTypeRts getBoundness() {
        return lastMessage.getBoundness();
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getTextShortTrimmed() {
        return lastMessage.getTextShortTrimmed();
    }

    public boolean isUnread() {
        return unread;
    }

    public List<String> getAttachments() {
        return attachments == null ? Collections.<String>emptyList() : attachments;
    }

    public boolean isBlockedByBuyer() {
        return blockedByBuyer;
    }

    public boolean isBlockedBySeller() {
        return blockedBySeller;
    }
}
