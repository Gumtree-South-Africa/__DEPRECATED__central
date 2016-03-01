package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;

public class ExtendedConversationEventSerializerTest {

    private static final String AD_ID = "9876345";
    private static final String CONVERSATION_ID = "conv-5801";
    private static final String MESSAGE_ID = "message-5801a";
    private static final DateTime TIMESTAMP = new DateTime(2012, 1, 10, 9, 11, 43);
    private static final ObjectReader objectReader = new ObjectMapper().reader();

    private static final NewConversationCommand newConversationCommand =
            NewConversationCommandBuilder.aNewConversationCommand(CONVERSATION_ID).
                    withAdId(AD_ID).
                    withBuyer("john@ymail.com", "ss983e9s0f7ds").
                    withSeller("mary@gmail.com", "x6cvm8dp9y3k9").
                    withCreatedAt(TIMESTAMP).
                    withState(ConversationState.ACTIVE).
                    withCustomValues(ImmutableMap.<String, String>builder()
                            .put("Custom-Value1", "blabla123")
                            .put("Custom-Value2", "more text with newlines \n and šⓣŕáñǵÈ characters")
                            .build()).
                    build();
    private static final AddMessageCommand addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand(CONVERSATION_ID, MESSAGE_ID)
            .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
            .withPlainTextBody("This is the body.\nIt contains newlines, \t tabs and some šⓣŕáñǵÈ characters.\n")
            .withReceivedAt(TIMESTAMP)
            .withHeaders(new HashMap<>())
            .build();

    private ExtendedConversationEventSerializer serializer = new ExtendedConversationEventSerializer();

    @Test
    public void testSerialize_newConversation() throws Exception {
        List<ConversationEvent> newConversationEvents = ImmutableConversation.apply(newConversationCommand);
        Assert.assertEquals(1, newConversationEvents.size());
        ConversationEvent newConversationEvent = newConversationEvents.get(0);
        DefaultMutableConversation conversation = DefaultMutableConversation.create(newConversationCommand);

        ExtendedConversationEvent newConversationExtendedEvent = new ExtendedConversationEvent(
                conversation, newConversationEvent,
                "ss983e9s0f7ds.seller.anon@platform.com", "x6cvm8dp9y3k9.buyer.anon@platform.com");

        byte[] newConversationSerialized = serializer.serialize(newConversationExtendedEvent);

        JsonNode expected = objectReader.readTree(getClass().getResourceAsStream("expectedNewConversation.json"));
        JsonNode actual = objectReader.readTree(new ByteArrayInputStream(newConversationSerialized));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSerialize_addMessage() throws Exception {
        DefaultMutableConversation conversation = DefaultMutableConversation.create(newConversationCommand);

        List<ConversationEvent> addMessageEvents = ((ImmutableConversation) conversation.getImmutableConversation()).apply(addMessageCommand);
        Assert.assertEquals(1, addMessageEvents.size());
        ConversationEvent addMessageEvent = addMessageEvents.get(0);

        conversation.applyCommand(addMessageCommand);

        ExtendedConversationEvent addMessageExtendedEvent = new ExtendedConversationEvent(
                conversation, addMessageEvent,
                "ss983e9s0f7ds.seller.anon@platform.com", "x6cvm8dp9y3k9.buyer.anon@platform.com");

        byte[] newConversationSerialized = serializer.serialize(addMessageExtendedEvent);

        JsonNode expected = objectReader.readTree(getClass().getResourceAsStream("expectedAddMessage.json"));
        JsonNode actual = objectReader.readTree(new ByteArrayInputStream(newConversationSerialized));
        Assert.assertEquals(expected, actual);
    }

}
