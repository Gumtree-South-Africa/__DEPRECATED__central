package com.ecg.de.ebayk.messagecenter.persistence;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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

    private final String adId;
    private final String conversationId;
    private final DateTime createdAt;
    private final DateTime modifiedAt;
    private final boolean containsUnreadMessages;
    private final DateTime receivedAt;

    //introduced later therefore Option to be compatible with persistent data
    private Optional<String> previewLastMessage;
    private Optional<String> buyerName;
    private Optional<String> sellerName;
    private Optional<String> buyerId;
    private Optional<String> messageDirection;
    private Optional<String> robot;
    private Optional<String> offerId;


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
            Optional<String> robot,
            Optional<String> offerId) {

        Preconditions.checkNotNull(adId);
        Preconditions.checkNotNull(conversationId);
        Preconditions.checkNotNull(createdAt);
        Preconditions.checkNotNull(modifiedAt);

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
        this.robot = robot;
        this.offerId = offerId;
    }

    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = Optional.fromNullable(message).or(previewLastMessage);
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection, robot, offerId);
    }

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

    public DateTime getReceivedAt() {
        return receivedAt;
    }

    public Optional<String> getSellerName() {
        return sellerName;
    }

    public Optional<String> getRobot() { return robot; }

    public Optional<String> getOfferId() {
        return offerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationThread that = (ConversationThread) o;

        return pairsAreEqual(
                containsUnreadMessages, that.containsUnreadMessages,
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
                robot, that.robot,
                offerId, that.offerId
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
                containsUnreadMessages,
                previewLastMessage,
                buyerName,
                sellerName,
                buyerId,
                messageDirection,
                robot,
                offerId);
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("adId", adId)
                .add("conversationId", conversationId)
                .add("createdAt", createdAt)
                .add("modifiedAt", modifiedAt)
                .add("containsUnreadMessages", containsUnreadMessages)
                .add("robot", robot)
                .add("offerId", offerId)
                .toString();
    }
}
