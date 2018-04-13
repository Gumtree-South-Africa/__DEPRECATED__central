package com.ecg.comaas.gtuk.listener.reporting;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationBuilder {
        private String conversationId;
        private ConversationState conversationState;
        private String advertId;
        private String sellerId;
        private String buyerId;
        private List<Message> messages = new ArrayList<>();
        private Map<String, String> customValues = new HashMap<>();
        private DateTime createdAt;
        private DateTime lastModifiedAt;

        public ConversationBuilder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ConversationBuilder customCategoryId(String customCategoryId) {
            this.customValues.put("categoryid", customCategoryId);
            return this;
        }

        public ConversationBuilder customBuyerIp(String customBuyerIp) {
            this.customValues.put("buyerip", customBuyerIp);
            return this;
        }

        public ConversationBuilder conversationState(ConversationState conversationState) {
            this.conversationState = conversationState;
            return this;
        }

        public ConversationBuilder advertId(String advertId) {
            this.advertId = advertId;
            return this;
        }

        public ConversationBuilder sellerId(String sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public ConversationBuilder buyerId(String buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public ConversationBuilder addMessage(Message message) {
            messages.add(message);
            return this;
        }

        public ConversationBuilder addMessage(MessageBuilder message) {
            messages.add(message.createMessage());
            return this;
        }

        public ConversationBuilder createdAt(DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ConversationBuilder lastModifiedAt(DateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Conversation createConversation() {
           return ImmutableConversation.Builder.aConversation()
                    .withId(conversationId)
                    .withState(conversationState)
                    .withAdId(advertId)
                    .withSeller(sellerId, "")
                    .withBuyer(buyerId, "")
                    .withMessages(messages)
                    .withCustomValues(customValues)
                    .withCreatedAt(createdAt)
                    .withLastModifiedAt(lastModifiedAt)
                    .build();
        }
    }