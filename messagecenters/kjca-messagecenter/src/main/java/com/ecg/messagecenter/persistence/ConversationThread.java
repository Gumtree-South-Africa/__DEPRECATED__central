package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Optional;

import static org.joda.time.DateTimeZone.UTC;

@JsonIgnoreProperties(ignoreUnknown = true, value = "containsUnreadMessages")
public class ConversationThread extends AbstractConversationThread {
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
            @JsonProperty("messageDirection") Optional<String> messageDirection) {
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
    }

    @Override
    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = message == null ? previewLastMessage : Optional.of(message);
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(UTC), DateTime.now(UTC), true, actualMessage, buyerName, sellerName, buyerId, messageDirection);
    }
}
