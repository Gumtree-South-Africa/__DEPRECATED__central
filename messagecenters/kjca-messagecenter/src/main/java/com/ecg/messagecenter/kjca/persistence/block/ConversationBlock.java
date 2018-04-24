package com.ecg.messagecenter.kjca.persistence.block;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.Optional;

/**
 * Defines the state of blocking between users in a conversation.
 * Both seller and buyer could block each other at different times.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationBlock {
    public static final int LATEST_VERSION = 1;

    private final String conversationId; // This is the key in Riak's bucket. It doesn't appear in JSON.
    private final int version;
    private final Optional<DateTime> buyerBlockedSellerAt;
    private final Optional<DateTime> sellerBlockedBuyerAt;

    @JsonCreator
    public ConversationBlock(
            @JsonProperty("conversationId") String conversationId,
            @JsonProperty("version") int version,
            @JsonProperty("buyerBlockedSellerAt") Optional<DateTime> buyerBlockedSellerAt,
            @JsonProperty("sellerBlockedBuyerAt") Optional<DateTime> sellerBlockedBuyerAt) {
        this.conversationId = conversationId;
        this.version = version;
        this.buyerBlockedSellerAt = buyerBlockedSellerAt;
        this.sellerBlockedBuyerAt = sellerBlockedBuyerAt;
    }

    public Optional<DateTime> getBuyerBlockedSellerAt() {
        return buyerBlockedSellerAt;
    }

    public Optional<DateTime> getSellerBlockedBuyerAt() {
        return sellerBlockedBuyerAt;
    }

    public int getVersion() {
        return version;
    }

    public String getConversationId() {
        return conversationId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConversationBlock{");
        sb.append("conversationId='").append(conversationId).append('\'');
        sb.append(", buyerBlockedSellerAt=").append(buyerBlockedSellerAt);
        sb.append(", sellerBlockerBuyerAt=").append(sellerBlockedBuyerAt);
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationBlock that = (ConversationBlock) o;
        return Objects.equal(version, that.version) &&
                Objects.equal(conversationId, that.conversationId) &&
                Objects.equal(buyerBlockedSellerAt, that.buyerBlockedSellerAt) &&
                Objects.equal(sellerBlockedBuyerAt, that.sellerBlockedBuyerAt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(conversationId, version, buyerBlockedSellerAt, sellerBlockedBuyerAt);
    }
}
