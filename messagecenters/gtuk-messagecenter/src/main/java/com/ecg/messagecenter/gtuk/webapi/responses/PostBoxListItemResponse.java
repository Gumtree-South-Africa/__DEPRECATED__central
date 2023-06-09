package com.ecg.messagecenter.gtuk.webapi.responses;

import com.ecg.gumtree.replyts2.common.message.MessageCenterUtils;
import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.gtuk.persistence.ConversationThread;
import com.ecg.messagecenter.gtuk.util.MessagesDiffer;
import com.ecg.messagecenter.gtuk.util.MessagesResponseFactory;
import com.ecg.messagecenter.gtuk.webapi.ConversationCustomValue;
import com.ecg.messagecenter.gtuk.webapi.DeletedCustomValue;
import com.ecg.messagecenter.gtuk.webapi.FlaggedCustomValue;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {
    private String email;
    private String id;
    private Long negotiationId;
    private String buyerName;
    private String sellerName;
    private String adId;

    private ConversationRole role;
    private boolean unread;
    private List<String> attachments = Collections.emptyList();

    private MessageResponse lastMessage;

    private String flaggedBuyer;
    private String flaggedSeller;
    private String deletedBuyer;
    private String deletedSeller;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String email, ConversationThread conversationThread, Conversation conversationRts) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(), "Only supported for data stored as list-aggregate");

        this.email = email;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ? conversationThread.getBuyerName().get() : "";
        this.sellerName = conversationThread.getSellerName().isPresent() ? conversationThread.getSellerName().get() : "";
        this.adId = conversationThread.getAdId();
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);

        this.lastMessage = new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getReceivedAt()),
                null,
                ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().get(),
                Optional.empty(),
                Collections.emptyList(),
                getSenderId(conversationThread),
                conversationThread.getBuyerId().orElse(null)
        );
        populateCustomValues(conversationRts);
    }

    private String getSenderId(ConversationThread conversationThread) {
        String messageDirection = conversationThread.getMessageDirection().get();
        return (MessageDirection.BUYER_TO_SELLER.toString().equals(messageDirection)) ?
                conversationThread.getBuyerId().orElse(null) : conversationThread.getSellerId().orElse(null);
    }

    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(
            String email,
            boolean isUnread,
            Conversation conversationRts) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
        response.email = email;
        response.unread = isUnread;
        response.id = conversationRts.getId();
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null
                ? ""
                : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null
                ? ""
                : conversationRts.getCustomValues().get("seller-name");
        response.adId = conversationRts.getAdId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);

        response.populateCustomValues(conversationRts);

        MessagesResponseFactory factory = new MessagesResponseFactory(new MessagesDiffer());
        Optional<MessageResponse> messageResponse = factory.latestMessage(email, conversationRts);
        if (messageResponse.isPresent()) {
            response.lastMessage = messageResponse.get();
            return Optional.of(response);

        } else {
            return Optional.empty();
        }
    }

    private void populateCustomValues(Conversation conversation) {
        this.flaggedBuyer = conversation.getCustomValues().get(
                new FlaggedCustomValue(ConversationRole.Buyer, ConversationCustomValue.AT_POSTFIX).keyName());
        this.flaggedSeller = conversation.getCustomValues().get(
                new FlaggedCustomValue(ConversationRole.Seller, ConversationCustomValue.AT_POSTFIX).keyName());
        this.deletedBuyer = conversation.getCustomValues().get(
                new DeletedCustomValue(ConversationRole.Buyer, ConversationCustomValue.AT_POSTFIX).keyName());
        this.deletedSeller = conversation.getCustomValues().get(
                new DeletedCustomValue(ConversationRole.Seller, ConversationCustomValue.AT_POSTFIX).keyName());
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

    public String getAdId() {
        return adId;
    }

    public String getReceivedDate() {
        return lastMessage.getReceivedDate();
    }

    public String getSenderEmail() {
        return lastMessage.getSenderEmail();
    }

    public String getBuyerEmail() {
        return lastMessage.getBuyerEmail();
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

    public String getFlaggedBuyer() {
        return flaggedBuyer;
    }

    public String getFlaggedSeller() {
        return flaggedSeller;
    }

    public String getDeletedBuyer() {
        return deletedBuyer;
    }

    public String getDeletedSeller() {
        return deletedSeller;
    }
}