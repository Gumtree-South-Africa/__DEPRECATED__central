package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PostBoxListItemResponse {

    private String email;
    private String userId;
    private String id;
    private String buyerName;
    private String sellerName;
    private Long userIdBuyer;
    private Long userIdSeller;
    private String adId;

    private ConversationRole role;
    private int numUnreadMessages;
    private boolean unread;
    private final List<String> attachments = Collections.emptyList();

    private MessageResponse lastMessage;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String userId, ConversationThread conversationThread, UserIdentifierService userIdentifierService) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(), "Only supported for data stored as list-aggregate");

        this.email = userId;
        this.userId = userId;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.numUnreadMessages = conversationThread.getNumUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ? conversationThread.getBuyerName().get() : "";
        this.sellerName = conversationThread.getSellerName().isPresent() ? conversationThread.getSellerName().get() : "";
        this.adId = conversationThread.getAdId();
        this.role = userIdentifierService.getRoleFromConversation(userId, conversationThread);
        this.userIdBuyer = conversationThread.getUserIdBuyer().orElse(null);
        this.userIdSeller = conversationThread.getUserIdSeller().orElse(null);

        this.lastMessage = new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getLastMessageCreatedAt().orElse(conversationThread.getReceivedAt())),
                ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().get(),
                Collections.emptyList());
    }

    // old style lookup when we didn't have a complete search aggregate on the list-view
    // todo: marked on calendar around June we deprecated this and throw it out (work with '...' placeholders if not data available)
    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(String userId, int numUnreadMessages, Conversation conversationRts, UserIdentifierService userIdentifierService) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
        response.email = userId;
        response.userId = userId;
        response.unread = numUnreadMessages > 0;
        response.numUnreadMessages = numUnreadMessages;
        response.id = conversationRts.getId();
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.userIdBuyer = conversationRts.getCustomValues().get(userIdentifierService.getBuyerUserIdName()) == null ? null : Long.parseLong(conversationRts.getCustomValues().get(userIdentifierService.getBuyerUserIdName()));
        response.userIdSeller = (conversationRts.getCustomValues().get("user-id-seller") == null) ? null : Long.parseLong(conversationRts.getCustomValues().get("user-id-seller"));

        response.adId = conversationRts.getAdId();
        response.role = userIdentifierService.getRoleFromConversation(userId, conversationRts);

        MessagesResponseFactory factory = new MessagesResponseFactory(userIdentifierService);
        Optional<MessageResponse> messageResponse = factory.latestMessage(userId, conversationRts);
        if (messageResponse.isPresent()) {
            response.lastMessage = messageResponse.get();
            return Optional.of(response);

        } else {
            return Optional.empty();
        }
    }

    /**
     * May contain user id instead of e-mail address
     */
    @Deprecated
    public String getEmail() {
        return email;
    }

    public String getUserId() {
        return userId;
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

    public int getNumUnreadMessages() {
        return numUnreadMessages;
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