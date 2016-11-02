package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {

    private String email;
    private String id;
    private String buyerName;
    private String sellerName;
    private String buyerId;
    private String sellerId;
    private String adId;

    private ConversationRole role;
    private boolean unread;
    private List<String> attachments = Collections.emptyList();

    private MessageResponse lastMessage;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String email, ConversationThread conversationThread) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(),"Only supported for data stored as list-aggregate");

        this.email = email;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ? conversationThread.getBuyerName().get(): "";
        this.sellerName = conversationThread.getSellerName().isPresent() ? conversationThread.getSellerName().get(): "";
        this.adId = conversationThread.getAdId();
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        this.buyerId = conversationThread.getBuyerId().orElse("");
        this.sellerId = conversationThread.getSellerId().orElse("");
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
                  Optional.ofNullable(conversationThread.getLastMessageId()).orElse(Optional.<String>empty()).orElse(null)),
                Collections.emptyList(),
                Optional.<RobotMessageResponse>empty());
    }

    // old style lookup when we didn't have a complete search aggregate on the list-view
    // todo: marked on calendar around June we deprecated this and throw it out (work with '...' placeholders if not data available)
    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(String email, boolean isUnread, Conversation conversationRts) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
        response.email = email;
        response.unread = isUnread;
        response.id = conversationRts.getId();
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.adId = conversationRts.getAdId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        response.sellerId = conversationRts.getSellerId();
        response.buyerId = conversationRts.getBuyerId();

        MessagesResponseFactory factory = new MessagesResponseFactory(new MessagesDiffer());
        Optional<MessageResponse> messageResponse = factory.latestMessage(email, conversationRts);
        if (messageResponse.isPresent()) {
            response.lastMessage = messageResponse.get();
            return Optional.of(response);

        } else {
            return Optional.empty();
        }
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
        return attachments == null ? Collections.emptyList() : attachments;
    }

    public String getRobot() {
        return lastMessage.getRobot();
    }
    public String getOfferId() {
        return lastMessage.getOfferId();
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }
}
