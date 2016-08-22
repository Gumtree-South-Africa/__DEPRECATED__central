package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.util.Assert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Event for a new conversation.
 */
public class ConversationCreatedEvent extends ConversationEvent {
    private final String conversationId;
    private final String adId;
    private final String buyerId;
    private final String sellerId;
    private final String buyerSecret;
    private final String sellerSecret;
    private final DateTime createdAt;
    private final ConversationState state;
    private final Map<String, String> customValues;

    @JsonCreator
    public ConversationCreatedEvent(
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("adId") String adId,
            @JsonProperty("buyerId") String buyerId,
            @JsonProperty("sellerId") String sellerId,
            @JsonProperty("buyerSecret") String buyerSecret,
            @JsonProperty("sellerSecret") String sellerSecret,
            @JsonProperty("createdAt") DateTime createdAt,
            @JsonProperty("state") ConversationState state,
            @JsonProperty("customValues") Map<String, String> customValues) {
        super("created-" + conversationId + "-" + createdAt.getMillis(), createdAt);

        Assert.notNull(conversationId);
        Assert.notNull(createdAt);
        Assert.notNull(state);

        this.conversationId = conversationId;
        this.adId = adId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerSecret = buyerSecret;
        this.sellerSecret = sellerSecret;
        this.createdAt = createdAt;
        this.state = state;
        this.customValues = ImmutableMap.copyOf(customValues);
    }

    public ConversationCreatedEvent(NewConversationCommand command) {
        this(
                command.getConversationId(),
                command.getAdId(),
                command.getBuyerId(),
                command.getSellerId(),
                command.getBuyerSecret(),
                command.getSellerSecret(),
                command.getCreatedAt(),
                command.getState(),
                command.getCustomValues()
        );
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getAdId() {
        return adId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getBuyerSecret() {
        return buyerSecret;
    }

    public String getSellerSecret() {
        return sellerSecret;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public ConversationState getState() {
        return state;
    }

    public Map<String, String> getCustomValues() {
        return customValues;
    }

    @Override // NOSONAR
    public boolean equals(Object o) { // NOSONAR
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;


        ConversationCreatedEvent that = (ConversationCreatedEvent) o;

        return Objects.equal(adId, that.adId) &&
                Objects.equal(buyerId, that.buyerId) &&
                Objects.equal(sellerId, that.sellerId) &&
                Objects.equal(buyerSecret, that.buyerSecret) &&
                Objects.equal(sellerSecret, that.sellerSecret) &&
                Objects.equal(conversationId, that.conversationId) &&
                Objects.equal(createdAt.getMillis(), that.createdAt.getMillis()) &&
                Objects.equal(customValues, that.customValues) &&
                Objects.equal(state, that.state) &&
                Objects.equal(getEventId(), that.getEventId()) &&
                Objects.equal(getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(conversationId, adId, buyerId, sellerId, buyerSecret, sellerSecret, createdAt.getMillis(), state, customValues, getEventId(), getConversationModifiedAt().getMillis());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("created", getConversationModifiedAt().toString())
                .add("adId", adId)
                .add("state", state).toString();
    }
}
