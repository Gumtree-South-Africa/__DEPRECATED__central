package com.ecg.comaas.gtuk.listener.eventpublisher.event;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class MessageReceivedEvent extends Event {

    private static final String EVENT_NAME = "MessageReceived";

    private Long advertId;
    private String conversationId;
    private MessageDirection messageDirection;
    private String buyerEmail;
    private String sellerEmail;
    private String text;

    private MessageReceivedEvent(Builder builder) {
        super(EVENT_NAME);
        this.advertId = builder.advertId;
        this.conversationId = builder.conversationId;
        this.messageDirection = builder.messageDirection;
        this.buyerEmail = builder.buyerEmail;
        this.sellerEmail = builder.sellerEmail;
        this.text = builder.text;
    }

    public static class Builder {

        private Long advertId;
        private String conversationId;
        private MessageDirection messageDirection;
        private String buyerEmail;
        private String sellerEmail;
        private String text;

        public Builder setAdvertId(Long advertId) {
            this.advertId = advertId;
            return this;
        }

        public Builder setConversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder setMessageDirection(MessageDirection messageDirection) {
            this.messageDirection = messageDirection;
            return this;
        }

        public Builder setBuyerEmail(String buyerEmail) {
            this.buyerEmail = buyerEmail;
            return this;
        }

        public Builder setSellerEmail(String sellerEmail) {
            this.sellerEmail = sellerEmail;
            return this;
        }

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public MessageReceivedEvent build() {
            return new MessageReceivedEvent(this);
        }
    }

    @Override
    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("advertId", this.advertId);
        node.put("conversationId", this.conversationId);
        node.put("messageDirection", this.messageDirection.toString());
        node.put("buyerEmail", this.buyerEmail);
        node.put("sellerEmail", this.sellerEmail);
        node.put("text", this.text);

        return node.toString();
    }

    @Override
    public String getEventLoggerFriendly() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("advertId", this.advertId);
        node.put("conversationId", this.conversationId);
        node.put("messageDirection", this.messageDirection.toString());

        return node.toString();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o){
            return true;
        }
        if(!(o instanceof MessageReceivedEvent)){
            return false;
        }

        MessageReceivedEvent that = (MessageReceivedEvent) o;

        if(advertId != null ? !advertId.equals(that.advertId) : that.advertId != null){
            return false;
        }
        if(buyerEmail != null ? !buyerEmail.equals(that.buyerEmail) : that.buyerEmail != null){
            return false;
        }
        if(conversationId != null ? !conversationId.equals(that.conversationId) : that.conversationId != null){
            return false;
        }
        if(messageDirection != that.messageDirection){
            return false;
        }
        if(sellerEmail != null ? !sellerEmail.equals(that.sellerEmail) : that.sellerEmail != null){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = advertId != null ? advertId.hashCode() : 0;
        result = 31 * result + (conversationId != null ? conversationId.hashCode() : 0);
        result = 31 * result + (messageDirection != null ? messageDirection.hashCode() : 0);
        result = 31 * result + (buyerEmail != null ? buyerEmail.hashCode() : 0);
        result = 31 * result + (sellerEmail != null ? sellerEmail.hashCode() : 0);
        return result;
    }
}
