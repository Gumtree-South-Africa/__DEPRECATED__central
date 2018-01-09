package com.ecg.messagecenter.persistence;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Optional;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

public abstract class AbstractConversationThread {
    protected final String adId;
    protected final String conversationId;
    protected final DateTime createdAt;
    protected final DateTime modifiedAt;
    protected final DateTime receivedAt;

    // Must be modifiable for implementations which store the unread count separate from the threads
    protected boolean containsUnreadMessages;

    // Introduced later and thus Optional to be compatible with persistent data
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
        Preconditions.checkNotNull(receivedAt);

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

    /*
     * Clone the ConversationThread set all conversations to read and change modifiedDate to now
     */
    public abstract AbstractConversationThread newReadConversation();

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

    public void setContainsUnreadMessages(boolean containsUnreadMessages) {
        this.containsUnreadMessages = containsUnreadMessages;
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
          adId, that.adId,
          conversationId, that.conversationId,
          createdAt.getMillis(), that.createdAt.getMillis(),
          modifiedAt.getMillis(), that.modifiedAt.getMillis(),
          receivedAt != null ? receivedAt.getMillis() : null, that.receivedAt != null ? that.receivedAt.getMillis() : null,
          previewLastMessage, that.previewLastMessage,
          buyerName, that.buyerName,
          sellerName, that.sellerName,
          buyerId, that.buyerId,
          messageDirection, that.messageDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(adId, conversationId, createdAt, modifiedAt, receivedAt, containsUnreadMessages, previewLastMessage, buyerName, sellerName, buyerId, messageDirection);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
          .add("modifiedAt", modifiedAt.toDateTime(DateTimeZone.UTC))
          .add("createdAt", createdAt.toDateTime(DateTimeZone.UTC))
          .add("adId", adId)
          .add("conversationId", conversationId)
          .add("containsUnreadMessages", containsUnreadMessages)
          .toString();
    }

    public String fullToString() {
        StringBuilder objstr = new StringBuilder(this.toString());

        objstr.append(MoreObjects.toStringHelper(this)
          .add("receivedAt", receivedAt != null ? receivedAt.toDateTime(DateTimeZone.UTC) : "")
          .add("previewLastMessage", previewLastMessage)
          .add("buyerName", buyerName)
          .add("sellerName", sellerName)
          .add("buyerId", buyerId)
          .add("messageDirection", messageDirection));

        return objstr.toString();
    }
}