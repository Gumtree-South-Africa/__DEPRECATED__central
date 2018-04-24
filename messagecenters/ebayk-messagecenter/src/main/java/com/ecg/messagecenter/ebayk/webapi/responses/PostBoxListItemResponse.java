package com.ecg.messagecenter.ebayk.webapi.responses;

import com.ecg.messagecenter.ebayk.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.ebayk.util.MessagesDiffer;
import com.ecg.messagecenter.ebayk.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {

    private String email;
    private String id;
    private String buyerName;
    private String sellerName;
    private Long userIdBuyer;
    private Long userIdSeller;
    private String adId;

    private ConversationRole role;
    private boolean unread;
    private final List<String> attachments = Collections.emptyList();

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
        this.userIdBuyer = conversationThread.getUserIdBuyer().orElse(null);
        this.userIdSeller = conversationThread.getUserIdSeller().orElse(null);

        this.lastMessage = new MessageResponse(
                null,
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getReceivedAt()),
                null,
                ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().get(),
                Optional.empty(),
                Collections.emptyList());
    }

    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(String email, boolean isUnread, Conversation conversationRts) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
        response.email = email;
        response.unread = isUnread;
        response.id = conversationRts.getId();
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.userIdBuyer = conversationRts.getCustomValues().get("user-id-buyer") == null ? null : Long.parseLong(conversationRts.getCustomValues().get("user-id-buyer"));
        response.userIdSeller = (conversationRts.getCustomValues().get("user-id-seller") == null) ? null : Long.parseLong(conversationRts.getCustomValues().get("user-id-seller"));

        response.adId = conversationRts.getAdId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);

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
        return attachments == null ? Collections.<String>emptyList() : attachments;
    }

    public Long getUserIdBuyer() {
        return userIdBuyer;
    }

    public Long getUserIdSeller() {
        return userIdSeller;
    }
}
