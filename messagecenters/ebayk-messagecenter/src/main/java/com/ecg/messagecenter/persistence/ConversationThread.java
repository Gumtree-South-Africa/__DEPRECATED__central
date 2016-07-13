package com.ecg.messagecenter.persistence;

import org.joda.time.DateTime;

import java.util.Optional;

public class ConversationThread extends AbstractConversationThread {
    private final Optional<Long> userIdBuyer;
    private final Optional<Long> userIdSeller;

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
            Optional<String> messageDirection,
            Optional<Long> userIdBuyer,
            Optional<Long> userIdSeller) {
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
