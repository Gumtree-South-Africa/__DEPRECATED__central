package com.gumtree;


import com.ecg.replyts.core.api.model.conversation.*;
import com.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by reweber on 11/05/15.
 */
public class MockFactory {
    private static final String MESSAGE_ID = "123";
    private static final String IP_ADDRESS = "1.1.1.1";

    public static Message mockMessage(MessageDirection direction) {
        return mockMessage(direction, null, null, MESSAGE_ID);
    }

    public static Message mockMessage(MessageDirection direction, Boolean buyerIsPro, Boolean sellerIsPro) {
        return mockMessage(direction, buyerIsPro, sellerIsPro, MESSAGE_ID);
    }

    public static Message mockMessage(MessageDirection direction, Boolean buyerIsPro, Boolean sellerIsPro, String messageId) {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getMessageDirection()).thenReturn(direction);
        Map<String, String> headers = new HashMap<>();
        headers.put(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), IP_ADDRESS);
        if (buyerIsPro != null) {
            headers.put(GumtreeCustomHeaders.BUYER_IS_PRO.getHeaderValue(), buyerIsPro.toString());
        }
        if (sellerIsPro != null) {
            headers.put(GumtreeCustomHeaders.SELLER_IS_PRO.getHeaderValue(), sellerIsPro.toString());
        }
        Mockito.when(message.getHeaders()).thenReturn(headers);
        Mockito.when(message.getId()).thenReturn(messageId);
        return message;
    }


    public static MutableConversation mockConversation(String buyerAddress, String sellerAddress, Message message) {
        return new ConversationBuilder().addMessage(message).withBuyer(buyerAddress).withSeller(sellerAddress).build();
    }

    public static class ConversationBuilder {

        private String buyerAddress;
        private String sellerAddress;
        private List<Message> messages = new ArrayList<>();
        private Map<String, String> conversationCustomHeaders = new HashMap<>();

        ConversationBuilder withBuyer(final String buyerAddress) {
            this.buyerAddress = buyerAddress;
            return this;
        }

        ConversationBuilder withSeller(final String sellerAddress) {
            this.sellerAddress = sellerAddress;
            return this;
        }

        ConversationBuilder addMessage(final Message message) {
            this.messages.add(message);
            return this;
        }

        public ConversationBuilder addHeader(final String headerName, final String headerValue) {
            conversationCustomHeaders.put(headerName, headerValue);
            return this;
        }

        public MutableConversation build() {
            Conversation conversation = Mockito.mock(Conversation.class);
            Mockito.when(conversation.getUserId(ConversationRole.Buyer)).thenReturn(buyerAddress);
            Mockito.when(conversation.getUserId(ConversationRole.Seller)).thenReturn(sellerAddress);
            Mockito.when(conversation.getCustomValues()).thenReturn(conversationCustomHeaders);
            MutableConversation mutableConversation = Mockito.mock(MutableConversation.class);
            Mockito.when(mutableConversation.getImmutableConversation()).thenReturn(conversation);
            for (Message message : messages) {
                Mockito.when(mutableConversation.getMessageById(message.getId())).thenReturn(message);
            }
            Mockito.when(mutableConversation.getMessages()).thenReturn(messages);
            return mutableConversation;
        }
    }
}
