package com.ecg.messagecenter.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.Optional;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

@JsonIgnoreProperties(ignoreUnknown = true, value = "containsUnreadMessages")
public class ConversationThread extends AbstractConversationThread {
    private Optional<String> robot;
    private Optional<String> offerId;

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
            @JsonProperty("robot") Optional<String> robot,
            @JsonProperty("offerId") Optional<String> offerId) {
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

        this.robot = robot;
        this.offerId = offerId;
    }

    @Override
    public ConversationThread sameButUnread(String message) {
        Optional<String> actualMessage = Optional.ofNullable(message);

        if (!actualMessage.isPresent())
            actualMessage = previewLastMessage;

        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection, robot, offerId);
    }

    public Optional<String> getRobot() { return robot; }

    public Optional<String> getOfferId() {
        return offerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ConversationThread that = (ConversationThread) o;

        return pairsAreEqual(
                robot, that.robot,
                offerId, that.offerId
        );
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hashCode(
                robot,
                offerId
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
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
