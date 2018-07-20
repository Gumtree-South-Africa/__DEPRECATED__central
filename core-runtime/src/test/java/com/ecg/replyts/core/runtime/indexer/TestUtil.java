package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;

public class TestUtil {

    public static ImmutableConversation.Builder makeConversation() {
        ImmutableConversation.Builder builder = aConversation().withId("id")
                .withCreatedAt(new DateTime(2012, 1, 29, 19, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 30, 19, 1, 55, DateTimeZone.forID("Europe/Amsterdam")))
                .withBuyer("Buyer@test.com", "buy3R")
                .withSeller("seller@Test.com", "s3ll3R")
                .withState(ConversationState.ACTIVE)
                .withAdId("myAd#123");

        return builder;
    }

    public static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder defaultMessage(String id) {
        return aMessage()
                .withId(id)
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("Subject", "Hello subject")
                .withTextParts(Collections.singletonList("some text"));
    }

}
