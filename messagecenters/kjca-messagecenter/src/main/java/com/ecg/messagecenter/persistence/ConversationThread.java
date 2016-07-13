package com.ecg.messagecenter.persistence;

import org.joda.time.DateTime;

import java.util.Optional;

import static org.joda.time.DateTimeZone.UTC;

public class ConversationThread extends AbstractConversationThread {
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
            Optional<String> messageDirection) {
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
