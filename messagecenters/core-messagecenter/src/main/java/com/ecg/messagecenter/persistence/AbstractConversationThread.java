package com.ecg.messagecenter.persistence;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import java.util.Optional;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

public abstract class AbstractConversationThread {
    protected final String adId;
    protected final String conversationId;
    protected final DateTime createdAt;
    protected final DateTime modifiedAt;
    protected final boolean containsUnreadMessages;
    protected final DateTime receivedAt;

    //introduced later therefore Option to be compatible with persistent data
    protected Optional<String> previewLastMessage;
    protected Optional<String> buyerName;
    protected Optional<String> sellerName;
    protected Optional<String> buyerId;
    protected Optional<String> messageDirection;

    public AbstractConversationThread(
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
        this.messageDirection = messageDirection;
    }

    public abstract AbstractConversationThread sameButUnread(String message);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractConversationThread that = (AbstractConversationThread) o;

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
        return MoreObjects.toStringHelper(this)
                .add("adId", adId)
                .add("conversationId", conversationId)
                .add("createdAt", createdAt)
                .add("modifiedAt", modifiedAt)
                .add("containsUnreadMessages", containsUnreadMessages)
                .toString();
    }
}
