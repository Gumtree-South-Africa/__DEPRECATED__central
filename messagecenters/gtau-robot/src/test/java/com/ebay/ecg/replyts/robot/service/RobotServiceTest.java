package com.ebay.ecg.replyts.robot.service;

import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessageSender;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RobotServiceTest {

    @Mock
    private MutableConversationRepository conversationRepository;

    @Mock
    private Guids guids;

    @Mock
    private HeldMailRepository heldMailRepository;

    @Mock
    private ConversationEventListeners conversationEventListeners;

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private RobotService robotService;

    @Test
    public void testHeadersAreSetCorrectlyForNoLinkMessage() throws Exception {
        MessagePayload payload = new MessagePayload();
        payload.setMessage("This is the simple message");

        MessagePayload.RichMessage message = new MessagePayload.RichMessage();
        message.setRichMessageText("This is a rich message");
        payload.setRichTextMessage(message);

        Conversation conversation = mock(Conversation.class);
        when(conversation.getAdId()).thenReturn("AD-ID");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

        Mail mail = robotService.aRobotMail(conversation, payload);

        assertEquals("AD-ID", mail.getAdId());
        assertEquals("GTAU", mail.getUniqueHeader("X-Robot"));
        assertEquals("noreply@gumtree.com.au", mail.getUniqueHeader("From"));
        assertEquals("AD-ID", mail.getUniqueHeader("X-ADID"));
        assertEquals("gumbot", mail.getUniqueHeader("X-Reply-Channel"));
        assertEquals("null", mail.getUniqueHeader("X-Message-Links"));
        assertEquals("This is a rich message", mail.getUniqueHeader("X-RichText-Message"));
        assertEquals("[]", mail.getUniqueHeader("X-RichText-Links"));
        assertNull(mail.getUniqueHeader("X-Message-Sender"));

    }

    @Test
    public void testHeadersAreSetCorrectlyForMessageWithSender() throws Exception {
        MessagePayload payload = new MessagePayload();
        payload.setMessage("This is the simple message");
        payload.setLinks(Collections.singletonList(new MessagePayload.Link("http://localhost", "EXTERNAL", 4, 5)));

        MessageSender sender = new MessageSender();
        sender.setName("The name");
        sender.addSenderIcon("test", "http://test");
        sender.addSenderIcon("test2", "http://test2");
        payload.setSender(sender);


        Conversation conversation = mock(Conversation.class);
        when(conversation.getAdId()).thenReturn("AD-ID");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

        Mail mail = robotService.aRobotMail(conversation, payload);

        assertEquals("AD-ID", mail.getAdId());
        assertEquals("GTAU", mail.getUniqueHeader("X-Robot"));
        assertEquals("noreply@gumtree.com.au", mail.getUniqueHeader("From"));
        assertEquals("AD-ID", mail.getUniqueHeader("X-ADID"));
        assertEquals("gumbot", mail.getUniqueHeader("X-Reply-Channel"));
        assertEquals("[{\"end\":5,\"start\":4,\"type\":\"EXTERNAL\",\"url\":\"http://localhost\"}]", mail.getUniqueHeader("X-Message-Links"));
        assertNull(mail.getUniqueHeader("X-RichText-Message"));
        assertNull(mail.getUniqueHeader("X-RichText-Links"));
        assertEquals("{\"name\":\"The name\",\"senderIcons\":[{\"name\":\"test\",\"url\":\"http://test\"},{\"name\":\"test2\",\"url\":\"http://test2\"}]}", mail.getUniqueHeader("X-Message-Sender"));
    }

    @Test
    public void addMessageToConversationStoresMail() throws Exception {
        String convId = "convId";
        String msgId = "msgId";
        MessagePayload payload = new MessagePayload() {{
            setMessage("msg");
            setMessageDirection(MessageDirection.BUYER_TO_SELLER.name());
        }};
        DefaultMutableConversation conversation = DefaultMutableConversation.create(new NewConversationCommand(convId, "1", "buyer@example.com", "seller@example.com", "sec1", "sec2", new DateTime(), ConversationState.ACTIVE, Maps.newHashMap()));

        when(conversationRepository.getById(convId)).thenReturn(conversation);
        when(guids.nextGuid()).thenReturn(msgId);

        robotService.addMessageToConversation(convId, payload);

        verify(heldMailRepository).write(eq(msgId), any(byte[].class));
    }
}