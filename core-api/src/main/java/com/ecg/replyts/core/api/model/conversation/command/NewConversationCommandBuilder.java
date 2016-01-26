package com.ecg.replyts.core.api.model.conversation.command;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for {@link NewConversationCommand}.
 */
public final class NewConversationCommandBuilder {
    private String conversationId;
    private String adId;
    private String buyerId;
    private String sellerId;
    private String buyerSecret;
    private String sellerSecret;
    private DateTime createdAt = new DateTime();
    private ConversationState state = ConversationState.ACTIVE;
    private Map<String, String> customValues = new HashMap<String, String>();

    private NewConversationCommandBuilder() {
    }

    public static NewConversationCommandBuilder aNewDeadConversationCommand(String conversationId) {
        NewConversationCommandBuilder result = new NewConversationCommandBuilder();
        result.conversationId = conversationId;
        result.state = ConversationState.DEAD_ON_ARRIVAL;
        return result;
    }

    public static NewConversationCommandBuilder aNewConversationCommand(String conversationId) {
        NewConversationCommandBuilder result = new NewConversationCommandBuilder();
        result.conversationId = conversationId;
        return result;
    }

    public NewConversationCommandBuilder withAdId(String adId) {
        this.adId = adId;
        return this;
    }

    public NewConversationCommandBuilder withBuyer(String buyerId, String buyerSecret) {
        this.buyerId = buyerId;
        this.buyerSecret = buyerSecret;
        return this;
    }

    public NewConversationCommandBuilder withSeller(String sellerId, String sellerSecret) {
        this.sellerId = sellerId;
        this.sellerSecret = sellerSecret;
        return this;
    }

    public NewConversationCommandBuilder withCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public NewConversationCommandBuilder withState(ConversationState state) {
        this.state = state;
        return this;
    }

    public NewConversationCommandBuilder withCustomValues(Map<String, String> customValues) {
        this.customValues = customValues;
        return this;
    }

    public NewConversationCommandBuilder addCustomValues(Map<String, String> customValues) {
        this.customValues.putAll(customValues);
        return this;
    }

    public NewConversationCommandBuilder addCustomValue(String key, String value) {
        this.customValues.put(key, value);
        return this;
    }

    public NewConversationCommand build() {
        return new NewConversationCommand(conversationId, adId, buyerId, sellerId, buyerSecret, sellerSecret, createdAt, state, customValues);
    }
}
