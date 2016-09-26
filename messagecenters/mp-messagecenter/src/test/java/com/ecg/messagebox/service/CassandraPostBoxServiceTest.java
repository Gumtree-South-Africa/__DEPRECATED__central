package com.ecg.messagebox.service;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPostBoxServiceTest {

    private static final String USER_ID_1 = "1";
    private static final String USER_ID_2 = "2";
    private static final String DEFAULT_SUBJECT = "Default subject";
    private static final String AD_ID = "m123";
    private static final String CONVERSATION_ID = "c1";

    private static final String BUYER_USER_ID_NAME = "user-id-buyer";
    private static final String SELLER_USER_ID_NAME = "user-id-seller";

    private static final String BUYER_NAME_KEY = "buyer-name";
    private static final String SELLER_NAME_KEY = "seller-name";

    private static final String BUYER_NAME_VALUE = "buyer";
    private static final String SELLER_NAME_VALUE = "seller";

    @Mock
    private CassandraPostBoxRepository conversationsRepo;
    @Mock
    private UserIdentifierService userIdentifierService;
    @Mock
    private BlockUserRepository blockUserRepo;

    private CassandraPostBoxService service;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        service = new CassandraPostBoxService(conversationsRepo, userIdentifierService, blockUserRepo);
        when(userIdentifierService.getBuyerUserIdName()).thenReturn(BUYER_USER_ID_NAME);
        when(userIdentifierService.getSellerUserIdName()).thenReturn(SELLER_USER_ID_NAME);
        when(blockUserRepo.areUsersBlocked(any(), any())).thenReturn(false);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void processNewMessageWithCorrectSubject() {
        when(conversationsRepo.getConversation(anyString(), anyString())).thenReturn(Optional.empty());

        Message rtsMsg1 = newMessage("1", SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);
        Message rtsMsg2 = newMessage("2", SELLER_TO_BUYER, MessageState.SENT, "Another subject");
        Conversation conversation = newConversationWithMessages(CONVERSATION_ID, singletonList(rtsMsg1)).build();
        service.processNewMessage(USER_ID_1, conversation, rtsMsg2, true);

        ArgumentCaptor<ConversationThread> conversationThreadArgCaptor = ArgumentCaptor.forClass(ConversationThread.class);
        ArgumentCaptor<com.ecg.messagebox.model.Message> messageArgCaptor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);
        verify(conversationsRepo).createConversation(eq(USER_ID_1), conversationThreadArgCaptor.capture(), messageArgCaptor.capture(), anyBoolean());

        ConversationThread capturedConversationThread = conversationThreadArgCaptor.getValue();
        Assert.assertEquals(DEFAULT_SUBJECT, capturedConversationThread.getMetadata().getEmailSubject());
        Assert.assertEquals(CONVERSATION_ID, capturedConversationThread.getId());
        Assert.assertEquals(AD_ID, capturedConversationThread.getAdId());
        Assert.assertEquals(Visibility.ACTIVE, capturedConversationThread.getVisibility());
        Assert.assertEquals(MessageNotification.RECEIVE, capturedConversationThread.getMessageNotification());

        Assert.assertEquals(rtsMsg2.getEventTimeUUID().get(), messageArgCaptor.getValue().getId());
    }

    @Test
    public void processNewMessageWithMetadataHeader() {
        when(conversationsRepo.getConversation(USER_ID_1, "c1")).thenReturn(Optional.empty());

        Message rtsMsg = newMessageWithMetadata("1", SELLER_TO_BUYER, MessageState.SENT);

        com.ecg.messagebox.model.Message newMessage = new com.ecg.messagebox.model.Message(
                rtsMsg.getEventTimeUUID().get(), "text 123", USER_ID_2, MessageType.ASQ, "metadata");

        Conversation rtsConversation = newConversation("c1").withMessages(singletonList(rtsMsg)).build();

        List<Participant> participants = newArrayList(
                new Participant(USER_ID_1, BUYER_NAME_VALUE, rtsConversation.getBuyerId(), ParticipantRole.BUYER),
                new Participant(USER_ID_2, SELLER_NAME_VALUE, rtsConversation.getSellerId(), ParticipantRole.SELLER));

        ConversationThread conversation = new ConversationThread(
                rtsConversation.getId(),
                rtsConversation.getAdId(),
                Visibility.ACTIVE,
                MessageNotification.RECEIVE,
                participants,
                newMessage,
                new ConversationMetadata(now(), "subject"));

        service.processNewMessage(USER_ID_1, rtsConversation, rtsMsg, true);

        verify(conversationsRepo).createConversation(USER_ID_1, conversation, newMessage, true);
    }

    private ImmutableConversation.Builder newConversation(String id) {
        return aConversation()
                .withId(id)
                .withAdId(AD_ID)
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withCustomValues(ImmutableMap.of(BUYER_USER_ID_NAME, USER_ID_1, SELLER_USER_ID_NAME, USER_ID_2,
                        BUYER_NAME_KEY, BUYER_NAME_VALUE, SELLER_NAME_KEY, SELLER_NAME_VALUE));
    }

    private ImmutableConversation.Builder newConversationWithMessages(String id, List<Message> messageList) {
        return newConversation(id).withMessages(messageList);
    }

    private Message newMessage(String id, MessageDirection direction, MessageState state, String subject) {
        return aMessage()
                .withId(id)
                .withEventTimeUUID(Optional.of(UUID.randomUUID()))
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"))
                .withHeader("Subject", subject)
                .build();
    }

    private Message newMessageWithMetadata(String id, MessageDirection direction, MessageState state) {
        return aMessage()
                .withId(id)
                .withEventTimeUUID(Optional.of(UUIDs.timeBased()))
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withHeader("X-Message-Metadata", "metadata")
                .withHeader("Subject", "subject")
                .withTextParts(singletonList("text 123"))
                .build();
    }
}
