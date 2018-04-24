package com.ecg.messagecenter.kjca.persistence;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.fasterxml.jackson.annotation.JsonCreator;
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
    public ConversationThread newReadConversation() {
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(UTC), receivedAt, false, previewLastMessage, buyerName, sellerName, buyerId, messageDirection);
    }
}
