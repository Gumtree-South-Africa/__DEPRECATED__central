package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

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

    //introduced later therefore Option to be compatible with persistent data
    private Optional<String> previewLastMessage;
    private Optional<String> buyerName;
    private Optional<String> sellerName;
    private Optional<String> buyerId;
    private Optional<String> sellerId;
    private Optional<String> messageDirection;

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
            Optional<String> sellerId,
            Optional<String> messageDirection) {

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
        this.sellerId = sellerId;
        this.messageDirection = messageDirection;
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

    public Optional<String> getSellerId() {
        return sellerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConversationThread that = (ConversationThread) o;

        return Pairwise.pairsAreEqual(
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
                sellerId, that.sellerId,
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
                sellerId,
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
