package com.ecg.de.ebayk.messagecenter.webapi.responses;

import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.de.ebayk.messagecenter.util.MessagesDiffer;
import com.ecg.de.ebayk.messagecenter.util.MessagesResponseFactory;
import com.ecg.de.ebayk.messagecenter.webapi.DeletedCustomValue;
import com.ecg.de.ebayk.messagecenter.webapi.FlaggedCustomValue;
import com.ecg.gumtree.replyts2.common.message.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;

import static com.ecg.de.ebayk.messagecenter.webapi.ConversationCustomValue.AT_POSTFIX;

/**
 * User: maldana
 * Date: 30.10.13
 * Time: 17:14
 *
 * @author maldana@ebay.de
 */
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
                Optional.<String>absent(),
                Collections.<MessageResponse.Attachment>emptyList(),
                getSenderId(conversationThread),
                conversationThread.getBuyerId().orNull()
        );
        populateCustomValues(conversationRts);
    }

    private String getSenderId(ConversationThread conversationThread) {
        String messageDirection = conversationThread.getMessageDirection().get();
        return (MessageDirection.BUYER_TO_SELLER.toString().equals(messageDirection)) ?
                conversationThread.getBuyerId().orNull() : conversationThread.getSellerId().orNull();
    }

    // old style lookup when we didn't have a complete search aggregate on the list-view
    // todo: marked on calendar around June we deprecated this and throw it out (work with '...' placeholders if not data available)
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
            return Optional.absent();
        }
    }

    private void populateCustomValues(Conversation conversation) {
        this.flaggedBuyer = conversation.getCustomValues().get(
                new FlaggedCustomValue(ConversationRole.Buyer, AT_POSTFIX).keyName());
        this.flaggedSeller = conversation.getCustomValues().get(
                new FlaggedCustomValue(ConversationRole.Seller, AT_POSTFIX).keyName());
        this.deletedBuyer = conversation.getCustomValues().get(
                new DeletedCustomValue(ConversationRole.Buyer, AT_POSTFIX).keyName());
        this.deletedSeller = conversation.getCustomValues().get(
                new DeletedCustomValue(ConversationRole.Seller, AT_POSTFIX).keyName());
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
