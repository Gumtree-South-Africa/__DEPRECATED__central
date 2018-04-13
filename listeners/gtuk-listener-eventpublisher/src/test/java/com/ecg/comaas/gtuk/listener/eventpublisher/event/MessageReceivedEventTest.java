package com.ecg.comaas.gtuk.listener.eventpublisher.event;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageReceivedEventTest {

    private MessageReceivedEvent messageReceivedEvent;

    @Before
    public void setUp() throws Exception {
        messageReceivedEvent = new MessageReceivedEvent.Builder()
                .setAdvertId(1L)
                .setConversationId("2D4")
                .setMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .setBuyerEmail("Buyer")
                .setSellerEmail("Seller")
                .setText("Test")
                .build();
    }

    @Test
    public void testToJsonString() throws Exception {
        String expectedJson = EventTestUtils.aMessage();

        assertEquals(expectedJson, messageReceivedEvent.toJsonString());
    }
}