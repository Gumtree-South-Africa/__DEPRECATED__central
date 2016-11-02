package com.ebay.ecg.replyts.robot.service;

import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessageSender;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created by gafabic on 5/2/16.
 */
public class RobotServiceTest {

    private RobotService robotService;

    @Before
    public void setup() {
        this.robotService = new RobotService(
                Mockito.mock(MutableConversationRepository.class),
                Mockito.mock(ModerationService.class),
                Mockito.mock(MailRepository.class),
                Mockito.mock(SearchService.class),
                Mockito.mock(Guids.class)
            );
    }

    @Test
    public void testHeadersAreSetCorrectlyForNoLinkMessage() throws Exception {
        MessagePayload payload = new MessagePayload();
        payload.setMessage("This is the simple message");

        MessagePayload.RichMessage message = new MessagePayload.RichMessage();
        message.setRichMessageText("This is a rich message");
        payload.setRichTextMessage(message);

        Conversation conversation = Mockito.mock(Conversation.class);
        Mockito.when(conversation.getAdId()).thenReturn("AD-ID");
        Mockito.when(conversation.getSellerId()).thenReturn("seller@seller.com");
        Mockito.when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

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


        Conversation conversation = Mockito.mock(Conversation.class);
        Mockito.when(conversation.getAdId()).thenReturn("AD-ID");
        Mockito.when(conversation.getSellerId()).thenReturn("seller@seller.com");
        Mockito.when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

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
}

