package com.ecg.messagecenter.gtuk.persistence;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Optional;

public class ConversationThread extends AbstractConversationThread {
    private Optional<String> sellerId;

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
            @JsonProperty("sellerId") Optional<String> sellerId,
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

        this.sellerId = sellerId;
    }


    @Override
    public AbstractConversationThread newReadConversation() {
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), receivedAt, false, previewLastMessage, buyerName, sellerName, buyerId, sellerId, messageDirection);
    }

    public Optional<String> getSellerId() {
        return sellerId;
    }

    public void setSellerId(Optional<String> sellerId) {
        this.sellerId = sellerId;
    }
}
