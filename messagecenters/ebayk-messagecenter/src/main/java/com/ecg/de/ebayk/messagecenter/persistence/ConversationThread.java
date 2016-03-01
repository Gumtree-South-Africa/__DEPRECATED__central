package com.ecg.de.ebayk.messagecenter.persistence;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 15:40
 *
 * @author maldana@ebay.de
 */
public class ConversationThread {

    private final String adId;
    private final String conversationId;
    private final DateTime createdAt;
    private final DateTime modifiedAt;
    private final boolean containsUnreadMessages;
    private final DateTime receivedAt;
    private final Optional<Long> negotiationId;

    //introduced later therefore Option to be compatible with persistent data
    private final Optional<String> previewLastMessage;
    private final Optional<String> buyerName;
    private final Optional<String> sellerName;
    private final Optional<String> buyerId;
    private final Optional<String> messageDirection;
    private final Optional<Long> userIdBuyer;
    private final Optional<Long> userIdSeller;


    public ConversationThread(
            String adId,
            String conversationId,
            DateTime createdAt,
            DateTime modifiedAt,
            DateTime receivedAt,
            boolean containsUnreadMessages,
            Optional<String> previewLastMessage,
            Optional<String> buyerName,
            Optional<String> sellerName,
            Optional<String> buyerId,
            Optional<String> messageDirection,
            Optional<Long> negotiationId,
            Optional<Long> userIdBuyer,
            Optional<Long> userIdSeller) {

        Preconditions.checkNotNull(adId);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(createdAt);
        Preconditions.checkNotNull(modifiedAt);

        this.adId = adId;
        this.conversationId = conversationId;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.receivedAt = receivedAt;
        this.containsUnreadMessages = containsUnreadMessages;
        this.previewLastMessage = previewLastMessage;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.buyerId = buyerId;
        this.messageDirection = messageDirection;
        this.negotiationId = negotiationId;
        this.userIdBuyer = userIdBuyer;
        this.userIdSeller = userIdSeller;
    }

    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = Optional.fromNullable(message).or(previewLastMessage);
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection, negotiationId, userIdSeller, userIdBuyer);
    }

    public boolean containsNewListAggregateData() {
        return previewLastMessage.isPresent() && messageDirection.isPresent();
    }

    public String getAdId() {
        return adId;
    }

    public Optional<String> getMessageDirection() {
        return messageDirection;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Optional<String> getBuyerId() {
        return buyerId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<String> getPreviewLastMessage() {
        return previewLastMessage;
    }

    public Optional<String> getBuyerName() {
        return buyerName;
    }

    public DateTime getModifiedAt() {
        return modifiedAt;
    }

    public boolean isContainsUnreadMessages() {
        return containsUnreadMessages;
    }

    public DateTime getReceivedAt() {
        return receivedAt;
    }

    public Optional<String> getSellerName() {
        return sellerName;
    }

    public Optional<Long> getNegotiationId() {
        return negotiationId;
    }

    public Optional<Long> getUserIdSeller() {
        return userIdSeller;
    }

    public Optional<Long> getUserIdBuyer() {
        return userIdBuyer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationThread that = (ConversationThread) o;

        return pairsAreEqual(
                containsUnreadMessages, that.containsUnreadMessages,
                adId, that.adId,
                conversationId, that.conversationId,
                createdAt, that.createdAt,
                modifiedAt, that.modifiedAt,
                receivedAt, that.receivedAt,
                previewLastMessage, that.previewLastMessage,
                buyerName, that.buyerName,
                sellerName, that.sellerName,
                buyerId, that.buyerId,
                messageDirection, that.messageDirection
        );

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                adId,
                conversationId,
                createdAt,
                modifiedAt,
                receivedAt,
                containsUnreadMessages,
                previewLastMessage,
                buyerName,
                sellerName,
                buyerId,
                messageDirection);
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("adId", adId)
                .add("conversationId", conversationId)
                .add("createdAt", createdAt)
                .add("modifiedAt", modifiedAt)
                .add("containsUnreadMessages", containsUnreadMessages)
                .toString();
    }


}
