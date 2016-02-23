package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
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

    /**
     * {@value true} when this was changed since it was last read.
     */
    private final boolean modified;

    private final String adId;
    private final String conversationId;
    private final DateTime createdAt;
    private final DateTime modifiedAt;
    private final DateTime receivedAt;
    private final long numUnreadMessages;
    private final Optional<Long> negotiationId;

    // introduced later therefore Option to be compatible with persistent data
    private final Optional<String> previewLastMessage;
    private final Optional<DateTime> lastMessageCreatedAt;

    private final Optional<String> buyerName;
    private final Optional<String> sellerName;
    private final Optional<String> buyerId;
    private final Optional<String> messageDirection;
    private final Optional<Long> userIdBuyer;
    private final Optional<Long> userIdSeller;

    @JsonCreator
    public ConversationThread(
            @JsonProperty("adId") String adId,
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("createdAt") DateTime createdAt,
            @JsonProperty("modifiedAt") DateTime modifiedAt,
            @JsonProperty("receivedAt") DateTime receivedAt,
            @JsonProperty("numUnreadMessages") long numUnreadMessages,
            @JsonProperty("previewLastMessage") Optional<String> previewLastMessage,
            @JsonProperty("buyerName") Optional<String> buyerName,
            @JsonProperty("sellerName") Optional<String> sellerName,
            @JsonProperty("buyerId") Optional<String> buyerId,
            @JsonProperty("messageDirection") Optional<String> messageDirection,
            @JsonProperty("negotiationId") Optional<Long> negotiationId,
            @JsonProperty("userIdBuyer") Optional<Long> userIdBuyer,
            @JsonProperty("userIdSeller") Optional<Long> userIdSeller,
            @JsonProperty("lastMessageCreatedAt") Optional<DateTime> lastMessageCreatedAt) {

        this(false, adId, conversationId, createdAt, modifiedAt, receivedAt, numUnreadMessages, previewLastMessage, buyerName, sellerName, buyerId, messageDirection, negotiationId, userIdBuyer, userIdSeller, lastMessageCreatedAt);
    }

    public ConversationThread(
            boolean modified,
            String adId,
            String conversationId,
            DateTime createdAt,
            DateTime modifiedAt,
            DateTime receivedAt,
            long numUnreadMessages,
            Optional<String> previewLastMessage,
            Optional<String> buyerName,
            Optional<String> sellerName,
            Optional<String> buyerId,
            Optional<String> messageDirection,
            Optional<Long> negotiationId,
            Optional<Long> userIdBuyer,
            Optional<Long> userIdSeller,
            Optional<DateTime> lastMessageCreatedAt
    ) {

        Preconditions.checkNotNull(adId);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(createdAt);
        Preconditions.checkNotNull(modifiedAt);

        this.modified = modified;
        this.adId = adId;
        this.conversationId = conversationId;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.receivedAt = receivedAt;
        this.numUnreadMessages = numUnreadMessages;
        this.previewLastMessage = previewLastMessage;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.buyerId = buyerId;
        this.messageDirection = messageDirection;
        this.negotiationId = negotiationId;
        this.userIdBuyer = userIdBuyer;
        this.userIdSeller = userIdSeller;
        this.lastMessageCreatedAt = lastMessageCreatedAt.isPresent() ? lastMessageCreatedAt : receivedAt != null ? Optional.of(receivedAt) : Optional.<DateTime>absent();
    }

    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = Optional.fromNullable(message).or(previewLastMessage);
        return new ConversationThread(modified, adId, conversationId, createdAt, DateTime.now(), DateTime.now(), numUnreadMessages + 1, actualMessage, buyerName, sellerName, buyerId, messageDirection, negotiationId, userIdBuyer, userIdSeller, lastMessageCreatedAt);
    }

    public ConversationThread sameButRead() {
        return new ConversationThread(modified, adId, conversationId, createdAt, DateTime.now(), DateTime.now(), 0, previewLastMessage, buyerName, sellerName, buyerId, messageDirection, negotiationId, userIdBuyer, userIdSeller, lastMessageCreatedAt);
    }

    public boolean containsNewListAggregateData() {
        return previewLastMessage.isPresent() && messageDirection.isPresent();
    }

    public boolean isModified() {
        return modified;
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

    @JsonIgnore
    public boolean isContainsUnreadMessages() {
        return numUnreadMessages > 0;
    }

    public long getNumUnreadMessages() {
        return numUnreadMessages;
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

    public Optional<DateTime> getLastMessageCreatedAt() {
        return lastMessageCreatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationThread that = (ConversationThread) o;

        return pairsAreEqual(
                numUnreadMessages, that.numUnreadMessages,
                adId, that.adId,
                conversationId, that.conversationId,
                createdAt, that.createdAt,
                modifiedAt, that.modifiedAt,
                receivedAt, that.receivedAt,
                previewLastMessage, that.previewLastMessage,
                buyerName, that.buyerName,
                sellerName, that.sellerName,
                buyerId, that.buyerId,
                messageDirection, that.messageDirection,
                lastMessageCreatedAt, that.lastMessageCreatedAt
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
                numUnreadMessages,
                previewLastMessage,
                buyerName,
                sellerName,
                buyerId,
                messageDirection,
                lastMessageCreatedAt);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("adId", adId)
                .add("conversationId", conversationId)
                .add("createdAt", createdAt)
                .add("modifiedAt", modifiedAt)
                .add("numUnreadMessages", numUnreadMessages)
                .toString();
    }
}