package com.ecg.messagebox.listeners;

import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.ecg.replyts.core.api.model.conversation.ConversationState.*;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxUpdateListenerTest {
    @Mock
    private PostBoxService delegatorMock;

    @Mock
    private UserIdentifierService userIdentifierServiceMock;

    @Mock
    BlockUserRepository blockUserRepository;

    @Mock
    MessagesResponseFactory messagesResponseFactory;

    @Mock
    MessageAddedEventProcessor messageAddedEventProcessor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private PostBoxUpdateListener listener;

    private ImmutableConversation.Builder convBuilder;
    private ImmutableMessage.Builder msgBuilder;

    private static final String BUYER_USER_ID = "1";
    private static final String SELLER_USER_ID = "2";

    @Before
    public void setup() {
        listener = new PostBoxUpdateListener(delegatorMock, userIdentifierServiceMock, messageAddedEventProcessor,
                blockUserRepository, messagesResponseFactory);

        convBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId("cid")
                .withState(ACTIVE);
        msgBuilder = ImmutableMessage.Builder
                .aMessage()
                .withId("mid")
                .withState(SENT)
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(now(UTC))
                .withLastModifiedAt(now(UTC));

        when(userIdentifierServiceMock.getUserIdentificationOfConversation(any(), eq(ConversationRole.Buyer)))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getUserIdentificationOfConversation(any(), eq(ConversationRole.Seller)))
                .thenReturn(of(SELLER_USER_ID));
    }

    @Test
    public void validConvAndMsgFromBuyer_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();
        when(messagesResponseFactory.getCleanedMessage(conversation, message)).thenReturn("clean message");

        listener.messageProcessed(conversation, message);

        verify(blockUserRepository).areUsersBlocked(BUYER_USER_ID, SELLER_USER_ID);
        verify(messageAddedEventProcessor).publishMessageAddedEvent(conversation, message, "clean message");
        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Buyer);
        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Seller);
        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, "clean message");
        verify(delegatorMock).processNewMessage(SELLER_USER_ID, conversation, message, true, "clean message");
    }

    @Test
    public void doNothingIfUserIsBlocked_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();
        when(blockUserRepository.areUsersBlocked(BUYER_USER_ID, SELLER_USER_ID)).thenReturn(true);

        listener.messageProcessed(conversation, message);


        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Buyer);
        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Seller);
        verifyNoMoreInteractions(userIdentifierServiceMock, delegatorMock);
    }

    @Test
    public void validConvAndMsgFromSeller_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withMessageDirection(SELLER_TO_BUYER).build();

        listener.messageProcessed(conversation, message);

        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Buyer);
        verify(userIdentifierServiceMock).getUserIdentificationOfConversation(conversation, ConversationRole.Seller);
        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, true, null);
        verify(delegatorMock).processNewMessage(SELLER_USER_ID, conversation, message, false, null);
    }

    @Test
    public void skipMessageCenter_notProcessed() {
        Conversation conversation = convBuilder.withCustomValues(ImmutableMap.of("skip-message-center", "true")).build();
        Message message = msgBuilder.build();

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(userIdentifierServiceMock, delegatorMock);
    }

    @Test
    public void convDeadOnArrival_notProcessed() {
        Conversation conversation = convBuilder.withState(DEAD_ON_ARRIVAL).build();
        Message message = msgBuilder.build();

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(userIdentifierServiceMock, delegatorMock);
    }

    @Test
    public void convClosed_processedForOwnMessageOnly() {
        Conversation conversation = convBuilder.withState(CLOSED).build();
        Message message = msgBuilder.withMessageDirection(BUYER_TO_SELLER).build();

        listener.messageProcessed(conversation, message);

        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, null);
        verify(delegatorMock, never()).processNewMessage(SELLER_USER_ID, conversation, message, true, null);
    }

    @Test
    public void noBuyerUserId_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getUserIdentificationOfConversation(any(), eq(ConversationRole.Buyer)))
                .thenReturn(empty());

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void noSellerUserId_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getUserIdentificationOfConversation(any(), eq(ConversationRole.Seller)))
                .thenReturn(empty());

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void ignoredMessage_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(IGNORED).build();

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void blockedOwnMessage_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(MessageState.BLOCKED).withMessageDirection(BUYER_TO_SELLER).build();

        listener.messageProcessed(conversation, message);

        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, null);
        verify(delegatorMock, never()).processNewMessage(SELLER_USER_ID, conversation, message, true, null);
    }

    @Test
    public void exceptionDuringProcessing_propagate() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(MessageState.BLOCKED).build();

        doThrow(new RuntimeException("~expected exception~"))
                .when(delegatorMock)
                .processNewMessage(BUYER_USER_ID, conversation, message, false, null);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error updating user projections for conversation cid, conversation state ACTIVE and message mid: ~expected exception~");

        listener.messageProcessed(conversation, message);
    }
}