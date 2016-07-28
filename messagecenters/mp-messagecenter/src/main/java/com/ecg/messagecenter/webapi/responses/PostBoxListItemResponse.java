package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public class PostBoxListItemResponse {

    private String id;
    private String buyerName;
    private String sellerName;
    private Long userIdBuyer;
    private Long userIdSeller;
    private String adId;
    private ConversationRole role;
    private int numUnreadMessages;
    private MessageResponse lastMessage;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String userId, ConversationThread conversationThread, UserIdentifierService userIdentifierService) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(), "Only supported for data stored as list-aggregate");

        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().orElse("");
        this.sellerName = conversationThread.getSellerName().orElse("");
        this.userIdBuyer = conversationThread.getUserIdBuyer().orElse(null);
        this.userIdSeller = conversationThread.getUserIdSeller().orElse(null);
        this.adId = conversationThread.getAdId();
        this.role = userIdentifierService.getRoleFromConversation(userId, conversationThread);
        this.numUnreadMessages = conversationThread.getNumUnreadMessages();

        this.lastMessage = new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getLastMessageCreatedAt().orElse(conversationThread.getReceivedAt())),
                ConversationBoundnessFinder.boundnessForRole(role, conversationThread.getMessageDirection().get()),
                conversationThread.getPreviewLastMessage().get());
    }

    public PostBoxListItemResponse(String conversationId, String buyerName, String sellerName, Long buyerUserId, Long sellerUserId,
                                   String adId, ConversationRole conversationRole, int numUnreadMessages, MessageResponse lastMsgResp) {
        this.id = conversationId;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.userIdBuyer = buyerUserId;
        this.userIdSeller = sellerUserId;
        this.adId = adId;
        this.role = conversationRole;
        this.numUnreadMessages = numUnreadMessages;
        this.lastMessage = lastMsgResp;
    }

    // old style lookup when we didn't have a complete search aggregate on the list-view
    // todo: marked on calendar around June we deprecated this and throw it out (work with '...' placeholders if not data available)
    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(String userId, int numUnreadMessages, Conversation conversationRts, UserIdentifierService userIdentifierService) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
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

    public String getId() {
        return id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public Long getUserIdBuyer() {
        return userIdBuyer;
    }

    public Long getUserIdSeller() {
        return userIdSeller;
    }

    public String getAdId() {
        return adId;
    }

    public ConversationRole getRole() {
        return role;
    }

    public int getNumUnreadMessages() {
        return numUnreadMessages;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBoxListItemResponse that = (PostBoxListItemResponse) o;
        return Objects.equals(id, that.id)
                && Objects.equals(buyerName, that.buyerName)
                && Objects.equals(sellerName, that.sellerName)
                && Objects.equals(userIdBuyer, that.userIdBuyer)
                && Objects.equals(userIdSeller, that.userIdSeller)
                && Objects.equals(adId, that.adId)
                && role == that.role
                && numUnreadMessages == that.numUnreadMessages
                && Objects.equals(lastMessage, that.lastMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, buyerName, sellerName, userIdBuyer, userIdSeller,
                adId, role, numUnreadMessages, lastMessage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("buyerName", buyerName)
                .add("sellerName", sellerName)
                .add("userIdBuyer", userIdBuyer)
                .add("userIdSeller", userIdSeller)
                .add("adId", adId)
                .add("role", role)
                .add("numUnreadMessages", numUnreadMessages)
                .add("lastMessage", lastMessage)
                .toString();
    }
}