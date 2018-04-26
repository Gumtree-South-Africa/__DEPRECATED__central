package com.ecg.messagecenter.gtau.robot.handler;

import com.ebay.ecg.australia.events.command.robot.RobotCommands;
import com.ebay.ecg.australia.events.entity.Entities;
import com.ebay.ecg.australia.events.origin.OriginDefinition;
import com.ecg.messagecenter.gtau.robot.api.requests.payload.Link;
import com.ecg.messagecenter.gtau.robot.api.requests.payload.MessagePayload;
import com.ecg.messagecenter.gtau.robot.service.RobotService;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by gafabic on 6/6/16.
 */
public class RabbitMQConsumerTest {

    private RabbitMQConsumer consumer;
    private RobotService robotService;

    @Before
    public void setup() {
        this.robotService = Mockito.mock(RobotService.class);
        this.consumer = new RabbitMQConsumer(robotService);
    }

    @Test
    public void testSimplePostMessageCommandCreatesCorrectPayload() throws Exception {
        final RobotCommands.PostMessageCommand postMessageCommand = RobotCommands.PostMessageCommand.newBuilder()
                .setOrigin(createDefaultOrigin())
                .setMessageInfo(Entities.MessageInfo.newBuilder()
                        .setConversationId("CONV-1")
                        .setMessage("This is a simple Gumbot Message")
                        .setMessageDirection(Entities.MessageDirection.BUYER_TO_SELLER)
                )
                .build();

        this.consumer.fire(postMessageCommand);

        ArgumentCaptor<String> convCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        Mockito.verify(robotService, Mockito.times(1)).addMessageToConversation(convCaptor.capture(), payloadCaptor.capture());

        assertEquals("CONV-1", convCaptor.getValue());

        MessagePayload payload = payloadCaptor.getValue();
        assertEquals("This is a simple Gumbot Message", payload.getMessage());
        assertEquals(MessageDirection.BUYER_TO_SELLER, payload.getMessageDirection());
        assertTrue(payload.getLinks().isEmpty());
        assertNull(payload.getSender());
        assertNull(payload.getRichTextMessage());
    }

    @Test
    public void testSimplePostMessageWithLinksCommandCreatesCorrectPayload() throws Exception {
        final RobotCommands.PostMessageCommand postMessageCommand = RobotCommands.PostMessageCommand.newBuilder()
                .setOrigin(createDefaultOrigin())
                .setMessageInfo(Entities.MessageInfo.newBuilder()
                        .setConversationId("CONV-2")
                        .setMessage("This is a simple Gumbot Message with links")
                        .setMessageDirection(Entities.MessageDirection.SELLER_TO_BUYER)
                        .addLinks(toLinkBuilder(2, 5, Entities.MessageLinkType.EXTERNAL, "http://localhost"))
                        .addLinks(toLinkBuilder(10, 12, Entities.MessageLinkType.EXTERNAL, "http://localhost-2"))
                )
                .build();

        this.consumer.fire(postMessageCommand);

        ArgumentCaptor<String> convCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        Mockito.verify(robotService, Mockito.times(1)).addMessageToConversation(convCaptor.capture(), payloadCaptor.capture());

        assertEquals("CONV-2", convCaptor.getValue());

        MessagePayload payload = payloadCaptor.getValue();
        assertEquals("This is a simple Gumbot Message with links", payload.getMessage());
        assertEquals(MessageDirection.SELLER_TO_BUYER, payload.getMessageDirection());

        assertEquals(2, payload.getLinks().size());
        verifyLink(2, 5, "http://localhost", payload.getLinks().get(0));
        verifyLink(10, 12, "http://localhost-2", payload.getLinks().get(1));

        assertNull(payload.getSender());
        assertNull(payload.getRichTextMessage());
    }

    @Test
    public void testPostMessageCommandWithSenderDetailsCreatesCorrectPayload() throws Exception {
        final RobotCommands.PostMessageCommand postMessageCommand = RobotCommands.PostMessageCommand.newBuilder()
                .setOrigin(createDefaultOrigin())
                .setMessageInfo(Entities.MessageInfo.newBuilder()
                        .setConversationId("CONV-3")
                        .setMessage("This is a simple Gumbot Message with Sender")
                        .setMessageDirection(Entities.MessageDirection.BUYER_TO_SELLER)
                        .setSender(Entities.MessageSenderInfo.newBuilder()
                                .setName("Gumbot")
                                .addIcon(Entities.MessageSenderIcon.newBuilder().setName("small").setSource("http://image"))
                        )
                )
                .build();

        this.consumer.fire(postMessageCommand);

        ArgumentCaptor<String> convCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        Mockito.verify(robotService, Mockito.times(1)).addMessageToConversation(convCaptor.capture(), payloadCaptor.capture());

        assertEquals("CONV-3", convCaptor.getValue());

        MessagePayload payload = payloadCaptor.getValue();
        assertEquals("This is a simple Gumbot Message with Sender", payload.getMessage());
        assertEquals(MessageDirection.BUYER_TO_SELLER, payload.getMessageDirection());

        assertTrue(payload.getLinks().isEmpty());

        assertEquals("Gumbot", payload.getSender().getName());
        assertEquals(1, payload.getSender().getSenderIcons().size());
        assertEquals("small", payload.getSender().getSenderIcons().get(0).getName());
        assertEquals("http://image", payload.getSender().getSenderIcons().get(0).getUrl());

        assertNull(payload.getRichTextMessage());
    }

    @Test
    public void testPostMessageRichTextWithLinksCreatesCorrectPayload() throws Exception {
        final RobotCommands.PostMessageCommand postMessageCommand = RobotCommands.PostMessageCommand.newBuilder()
                .setOrigin(createDefaultOrigin())
                .setMessageInfo(Entities.MessageInfo.newBuilder()
                        .setConversationId("CONV-3")
                        .setMessage("This is a simple Gumbot Message with links")
                        .setMessageDirection(Entities.MessageDirection.SELLER_TO_BUYER)
                        .addLinks(toLinkBuilder(2, 5, Entities.MessageLinkType.EXTERNAL, "http://localhost"))
                        .addLinks(toLinkBuilder(10, 12, Entities.MessageLinkType.EXTERNAL, "http://localhost-2"))
                        .setRichContentMessage(Entities.RichTextMessageInfo.newBuilder()
                                .setMessage("This is the rich text version of the message")
                                .addLinks(toLinkBuilder(2, 5, Entities.MessageLinkType.EXTERNAL, "http://localhost-3"))
                        )
                )
                .build();

        this.consumer.fire(postMessageCommand);

        ArgumentCaptor<String> convCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        Mockito.verify(robotService, Mockito.times(1)).addMessageToConversation(convCaptor.capture(), payloadCaptor.capture());

        assertEquals("CONV-3", convCaptor.getValue());

        MessagePayload payload = payloadCaptor.getValue();
        assertEquals("This is a simple Gumbot Message with links", payload.getMessage());
        assertEquals(MessageDirection.SELLER_TO_BUYER, payload.getMessageDirection());

        assertEquals(2, payload.getLinks().size());
        verifyLink(2, 5, "http://localhost", payload.getLinks().get(0));
        verifyLink(10, 12, "http://localhost-2", payload.getLinks().get(1));

        assertNull(payload.getSender());

        assertEquals("This is the rich text version of the message", payload.getRichTextMessage().getRichMessageText());
        assertEquals(1, payload.getRichTextMessage().getLinks().size());
        verifyLink(2, 5, "http://localhost-3", payload.getRichTextMessage().getLinks().get(0));
    }

    private OriginDefinition.Origin createDefaultOrigin() {
        return OriginDefinition.Origin.newBuilder()
                .setPlatform(OriginDefinition.Platform.WEB)
                .setTimestamp(System.currentTimeMillis())
                .setBatchRequest(OriginDefinition.BatchRequest.newBuilder().setBatchJobName("TEST"))
                .build();

    }

    private Entities.MessageLinkInfo.Builder toLinkBuilder(int beginIndex, int endIndex, Entities.MessageLinkType type,
                                                           String url) {
        return Entities.MessageLinkInfo.newBuilder()
                .setBeginIndex(beginIndex)
                .setEndIndex(endIndex)
                .setType(type)
                .setUrl(url);
    }

    private void verifyLink(int start, int end, String url, Link link) {
        assertEquals(start, link.getStart());
        assertEquals(end, link.getEnd());
        assertEquals("EXTERNAL", link.getType());
        assertEquals(url, link.getUrl());
    }
}

