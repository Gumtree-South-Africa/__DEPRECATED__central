package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        Optional<String> actualMessage = Optional.ofNullable(message);

        if (!actualMessage.isPresent())
            actualMessage = previewLastMessage;

        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection, userIdSeller, userIdBuyer);
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
}
