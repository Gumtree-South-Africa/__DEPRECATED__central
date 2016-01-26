package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Map;

/**
 * Command to create a new conversation.
 * <p/>
 * See also {@link NewConversationCommandBuilder}.
 */
public class NewConversationCommand extends ConversationCommand {
    private final String adId;
    private final String buyerId;
    private final String sellerId;
    private final String buyerSecret;
    private final String sellerSecret;
    private final DateTime createdAt;
    private final ConversationState state;
    private final Map<String, String> customValues;

    public NewConversationCommand(String conversationId, String adId, String buyerId, String sellerId, String buyerSecret, String sellerSecret, DateTime createdAt, ConversationState state, Map<String, String> customValues) {
        super(conversationId);

        if (createdAt == null) throw new IllegalArgumentException();
        if (state == null) throw new IllegalArgumentException();

        // Note: secrets may be null for backward compatibility with old anonymization schemes

        this.adId = adId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerSecret = buyerSecret;
        this.sellerSecret = sellerSecret;
        this.createdAt = createdAt;
        this.state = state;
        this.customValues = Collections.unmodifiableMap(customValues);
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
}
