package com.ecg.comaas.gtuk.listener.eventpublisher.event;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;

public class EventTestUtils {

    public static String aMessage() {
        return "{" +
                "\"advertId\":1," +
                "\"conversationId\":\"2D4\"," +
                "\"messageDirection\":\"BUYER_TO_SELLER\"," +
                "\"buyerEmail\":\"Buyer\"," +
                "\"sellerEmail\":\"Seller\"," +
                "\"text\":\"Test\"" +
                "}";
    }

    public static MessageReceivedEvent anEvent() {
        return new MessageReceivedEvent.Builder()
                .setAdvertId(1L)
                .setConversationId("2D4")
                .setMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .setBuyerEmail("Buyer")
                .setSellerEmail("Seller")
                .setText("Test")
                .build();
    }

    public static String aRawEmail() {

        return "Dear Ni,\n" +
                "\n" +
                "You have received a reply to your ad Bynum  posted in Used Aixam for salein=\n" +
                " Isle Of Arran.\n" +
                "\n" +
                "---------------------------------------------------------------------------=\n" +
                "-----\n" +
                "\n" +
                "From: Javier\n" +
                "\n" +
                "\n" +
                "\n" +
                "Test\n" +
                "\n" +
                "\n" +
                "---------------------------------------------------------------------------=\n" +
                "Need to respond? Answer this message by pressing 'Reply' and Gumtree will make sure your email" +
                " address stays hidden (we don't recommend that you share your email address, as this can sometimes" +
                " lead to bad stuff like fraud or spam).";
    }
}
