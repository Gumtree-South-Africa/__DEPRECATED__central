package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true, value = "containsUnreadMessages")
public class ConversationThread extends AbstractConversationThread {
    private final Optional<Long> userIdBuyer;
    private final Optional<Long> userIdSeller;

    @JsonCreator
    public ConversationThread(
            @JsonProperty("adId") String adId,
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("createdAt") DateTime createdAt,
            @JsonProperty("modifiedAt") DateTime modifiedAt,
            @JsonProperty("receivedAt") DateTime receivedAt,
            @JsonProperty("containsUnreadMessages") boolean containsUnreadMessages,
            @JsonProperty("previewLastMessage") Optional<String> previewLastMessage,
            @JsonProperty("buyerName") Optional<String> buyerName,
            @JsonProperty("sellerName") Optional<String> sellerName,
            @JsonProperty("buyerId") Optional<String> buyerId,
            @JsonProperty("messageDirection") Optional<String> messageDirection,
            @JsonProperty("userIdBuyer") Optional<Long> userIdBuyer,
            @JsonProperty("userIdSeller") Optional<Long> userIdSeller) {
        super(
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
                messageDirection
        );

        this.userIdBuyer = userIdBuyer;
        this.userIdSeller = userIdSeller;
    }

    @Override
    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = message == null ? previewLastMessage : Optional.of(message);
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection, userIdSeller, userIdBuyer);
    }

    @Override
    public ConversationThread sameButRead() {
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), receivedAt, false, previewLastMessage, buyerName, sellerName, buyerId, messageDirection, userIdSeller, userIdBuyer);
    }

    public boolean containsNewListAggregateData() {
        return previewLastMessage.isPresent() && messageDirection.isPresent();
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
        if (!super.equals(o)) return false;
        ConversationThread that = (ConversationThread) o;
        return Objects.equal(userIdBuyer, that.userIdBuyer) &&
                Objects.equal(userIdSeller, that.userIdSeller);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), userIdBuyer, userIdSeller);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.toString());
        sb.append("userIdBuyer=").append(userIdBuyer);
        sb.append(", userIdSeller=").append(userIdSeller);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String fullToString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.fullToString())
                .append("userIdBuyer=").append(userIdBuyer)
                .append("userIdSeller=").append(userIdSeller)
                .append("}");
        return sb.toString();
    }
}
