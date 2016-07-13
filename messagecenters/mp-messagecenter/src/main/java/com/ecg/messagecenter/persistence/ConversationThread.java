package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.Optional;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationThread extends AbstractConversationThread {
    private int numUnreadMessages;

    //introduced later therefore Option to be compatible with persistent data
    private final Optional<DateTime> lastMessageCreatedAt;

    private final Optional<Long> userIdBuyer;
    private final Optional<Long> userIdSeller;

    @JsonCreator
    public ConversationThread(
            @JsonProperty("adId") String adId,
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("createdAt") DateTime createdAt,
            @JsonProperty("modifiedAt") DateTime modifiedAt,
            @JsonProperty("receivedAt") DateTime receivedAt,
            // TODO numUnreadMessages should be removed from constructor. It should be serialized to json only for Riak and
            // not for Cassandra. For Cassandra the counter table should be used.
            @JsonProperty("numUnreadMessages") int numUnreadMessages,
            @JsonProperty("previewLastMessage") Optional<String> previewLastMessage,
            @JsonProperty("buyerName") Optional<String> buyerName,
            @JsonProperty("sellerName") Optional<String> sellerName,
            @JsonProperty("buyerId") Optional<String> buyerId,
            @JsonProperty("messageDirection") Optional<String> messageDirection,
            @JsonProperty("userIdBuyer") Optional<Long> userIdBuyer,
            @JsonProperty("userIdSeller") Optional<Long> userIdSeller,
            @JsonProperty("lastMessageCreatedAt") Optional<DateTime> lastMessageCreatedAt) {
        super(
          adId,
          conversationId,
          createdAt,
          modifiedAt,
          receivedAt,
          numUnreadMessages > 0,
          previewLastMessage,
          buyerName,
          sellerName,
          buyerId,
          messageDirection);

        this.numUnreadMessages = numUnreadMessages;
        this.userIdBuyer = userIdBuyer;
        this.userIdSeller = userIdSeller;
        this.lastMessageCreatedAt = lastMessageCreatedAt.isPresent() ? lastMessageCreatedAt : receivedAt != null ? Optional.of(receivedAt) : Optional.<DateTime>empty();
    }

    @Override
    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = message == null ? previewLastMessage : Optional.of(message);
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), numUnreadMessages + 1, actualMessage, buyerName, sellerName, buyerId, messageDirection, userIdBuyer, userIdSeller, lastMessageCreatedAt);
    }

    public ConversationThread sameButRead() {
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), 0, previewLastMessage, buyerName, sellerName, buyerId, messageDirection, userIdBuyer, userIdSeller, lastMessageCreatedAt);
    }

    @JsonIgnore
    public boolean isContainsUnreadMessages() {
        return numUnreadMessages > 0;
    }

    public int getNumUnreadMessages() {
        return numUnreadMessages;
    }

    public void setNumUnreadMessages(int numUnreadMessages) {
        this.numUnreadMessages = numUnreadMessages;
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
        if (!super.equals(o)) return false;

        ConversationThread that = (ConversationThread) o;

        return pairsAreEqual(
                numUnreadMessages, that.numUnreadMessages,
                lastMessageCreatedAt, that.lastMessageCreatedAt
        );
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(
                lastMessageCreatedAt
        );
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
