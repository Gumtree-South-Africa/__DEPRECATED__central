package com.ecg.messagecenter.gtau.robot.service;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ecg.messagecenter.gtau.robot.api.requests.payload.MessagePayload;
import com.ecg.messagecenter.gtau.robot.api.requests.payload.MessageSender;
import com.ecg.messagecenter.gtau.robot.api.requests.payload.RichMessage;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RobotServiceTest {

    @Mock
    private MutableConversationRepository conversationRepository;

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
        MessagePayload payload = new MessagePayload(
                "This is the simple message",
                MessageDirection.UNKNOWN,
                null,
                null,
                new RichMessage("This is a rich message", null)
        );

        Conversation conversation = mock(Conversation.class);
        when(conversation.getAdId()).thenReturn("AD-ID");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

        Mail mail = ContentUtils.buildMail(conversation, payload).get();

        assertThat(mail.getAdId()).isEqualTo("AD-ID");
        assertThat(mail.getUniqueHeader("X-Robot")).isEqualTo("GTAU");
        assertThat(mail.getUniqueHeader("From")).isEqualTo("noreply@gumtree.com.au");
        assertThat(mail.getUniqueHeader("X-ADID")).isEqualTo("AD-ID");
        assertThat(mail.getUniqueHeader("X-Reply-Channel")).isEqualTo("gumbot");
        assertThat(mail.getUniqueHeader("X-Message-Links")).isEqualTo("[]");
        assertThat(mail.getUniqueHeader("X-RichText-Message")).isEqualTo("This is a rich message");
        assertThat(mail.getUniqueHeader("X-RichText-Links")).isEqualTo("[]");
        assertThat(mail.getUniqueHeader("X-Message-Sender")).isNull();
    }

    @Test
    public void testHeadersAreSetCorrectlyForMessageWithSender() throws Exception {
        Entities.MessageLinkInfo messageLinkInfo = Entities.MessageLinkInfo.newBuilder()
                .setUrl("http://localhost")
                .setType(Entities.MessageLinkType.EXTERNAL)
                .setBeginIndex(4)
                .setEndIndex(5)
                .build();

        Entities.MessageSenderIcon icon1 = Entities.MessageSenderIcon.newBuilder().setName("test").setSource("http://test").build();
        Entities.MessageSenderIcon icon2 = Entities.MessageSenderIcon.newBuilder().setName("test2").setSource("http://test2").build();

        MessagePayload payload = new MessagePayload(
                "This is the simple message",
                MessageDirection.UNKNOWN,
                Collections.singletonList(messageLinkInfo),
                new MessageSender("The name", Arrays.asList(icon1, icon2)),
                null
        );

        Conversation conversation = mock(Conversation.class);
        when(conversation.getAdId()).thenReturn("AD-ID");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");

        Mail mail = ContentUtils.buildMail(conversation, payload).get();

        assertThat(mail.getAdId()).isEqualTo("AD-ID");
        assertThat(mail.getUniqueHeader("X-Robot")).isEqualTo("GTAU");
        assertThat(mail.getUniqueHeader("From")).isEqualTo("noreply@gumtree.com.au");
        assertThat(mail.getUniqueHeader("X-ADID")).isEqualTo("AD-ID");
        assertThat(mail.getUniqueHeader("X-Reply-Channel")).isEqualTo("gumbot");
        assertThat(mail.getUniqueHeader("X-Message-Links"))
                .isEqualTo("[{\"end\":5,\"start\":4,\"type\":\"EXTERNAL\",\"url\":\"http://localhost\"}]");
        assertThat(mail.getUniqueHeader("X-RichText-Message")).isNull();
        assertThat(mail.getUniqueHeader("X-RichText-Links")).isNull();
        assertThat(mail.getUniqueHeader("X-Message-Sender"))
                .isEqualTo("{\"name\":\"The name\",\"senderIcons\":[{\"name\":\"test\",\"url\":\"http://test\"},{\"name\":\"test2\",\"url\":\"http://test2\"}]}");
    }

    @Test
    public void addMessageToConversationStoresMail() throws Exception {
        String convId = "convId";
        MessagePayload payload = new MessagePayload("msg", MessageDirection.BUYER_TO_SELLER, null, null, null);
        DefaultMutableConversation conversation = DefaultMutableConversation.create(
                new NewConversationCommand(convId, "1", "buyer@example.com", "seller@example.com", "sec1", "sec2", new DateTime(), ConversationState.ACTIVE, Maps.newHashMap()));

        when(conversationRepository.getById(convId)).thenReturn(conversation);
        robotService.addMessageToConversation(convId, payload);
        verify(heldMailRepository).write(anyString(), any(byte[].class));
    }
}