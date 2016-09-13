package com.ecg.messagebox.service;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.persistence.cassandra.DefaultCassandraPostBoxRepository;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

public class CassandraPostBoxServiceTest {

    private static final String USER_ID_1 = "1";
    private static final String USER_ID_2 = "2";
    private static final String DEFAULT_SUBJECT = "Default subject";
    private static final String AD_ID = "m123";
    private static final String CONVERSATION_ID = "c1";

    private static final String BUYER_USER_ID_NAME = "user-id-buyer";
    private static final String SELLER_USER_ID_NAME = "user-id-seller";

    private final CassandraPostBoxRepository conversationsRepoMock = mock(DefaultCassandraPostBoxRepository.class);
    private final UserIdentifierService userIdentifierServiceMock = mock(UserIdentifierService.class);

    private CassandraPostBoxService service = new CassandraPostBoxService(conversationsRepoMock, userIdentifierServiceMock);

    @Test
    public void processNewMessageWithStateIgnored() {
        when(userIdentifierServiceMock.getBuyerUserIdName()).thenReturn(BUYER_USER_ID_NAME);

        Message rtsMsg = newMessage("1", MessageDirection.BUYER_TO_SELLER, MessageState.IGNORED, DEFAULT_SUBJECT);
        service.processNewMessage(USER_ID_1, newConversation(CONVERSATION_ID).build(), rtsMsg, true);

        verify(userIdentifierServiceMock).getBuyerUserIdName();
        verifyZeroInteractions(conversationsRepoMock);
    }

    @Test
    public void processNewMessageWithStateHeld() {
        when(userIdentifierServiceMock.getSellerUserIdName()).thenReturn(SELLER_USER_ID_NAME);

        Message rtsMsg = newMessage("1", MessageDirection.SELLER_TO_BUYER, MessageState.HELD, DEFAULT_SUBJECT);
        service.processNewMessage(USER_ID_1, newConversation(CONVERSATION_ID).build(), rtsMsg, true);

        verify(userIdentifierServiceMock).getSellerUserIdName();
        verifyZeroInteractions(conversationsRepoMock);
    }

    @Test
    public void processNewMessageWithCorrectSubject() {
        when(userIdentifierServiceMock.getSellerUserIdName()).thenReturn(SELLER_USER_ID_NAME);
        when(conversationsRepoMock.getConversation(anyString(), anyString())).thenReturn(Optional.empty());

        Message rtsMsg1 = newMessage("1", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);
        Message rtsMsg2 = newMessage("2", MessageDirection.SELLER_TO_BUYER, MessageState.SENT, "Another subject");
        Conversation conversation = newConversationWithMessages(CONVERSATION_ID, Arrays.asList(rtsMsg1)).build();
        service.processNewMessage(USER_ID_1, conversation, rtsMsg2, true);

        ArgumentCaptor<ConversationThread> conversationThreadArgCaptor = ArgumentCaptor.forClass(ConversationThread.class);
        ArgumentCaptor<com.ecg.messagebox.model.Message> messageArgCaptor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);
        verify(conversationsRepoMock).createConversation(eq(USER_ID_1), conversationThreadArgCaptor.capture(), messageArgCaptor.capture(), anyBoolean());

        ConversationThread capturedConversationThread = conversationThreadArgCaptor.getValue();
        Assert.assertEquals(DEFAULT_SUBJECT, capturedConversationThread.getMetadata().getEmailSubject());
        Assert.assertEquals(CONVERSATION_ID, capturedConversationThread.getId());
        Assert.assertEquals(AD_ID, capturedConversationThread.getAdId());
        Assert.assertEquals(Visibility.ACTIVE, capturedConversationThread.getVisibility());
        Assert.assertEquals(MessageNotification.RECEIVE, capturedConversationThread.getMessageNotification());

        Assert.assertEquals(rtsMsg2.getEventTimeUUID().get(), messageArgCaptor.getValue().getId());
    }

    private ImmutableConversation.Builder newConversation(String id) {
        return aConversation()
                .withId(id)
                .withAdId(AD_ID)
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withCustomValues(ImmutableMap.of(BUYER_USER_ID_NAME, USER_ID_1, SELLER_USER_ID_NAME, USER_ID_2));
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
}
