package com.ecg.messagebox.listeners;

import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.app.ContentOverridingPostProcessorService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.processing.MessagesResponseFactory;
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

import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.ConversationState.ACTIVE;
import static com.ecg.replyts.core.api.model.conversation.ConversationState.CLOSED;
import static com.ecg.replyts.core.api.model.conversation.ConversationState.DEAD_ON_ARRIVAL;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.IGNORED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxUpdateListenerTest {
    @Mock
    private PostBoxService delegatorMock;

    @Mock
    private UserIdentifierService userIdentifierServiceMock;

    @Mock
    private BlockUserRepository blockUserRepository;

    @Mock
    private MessagesResponseFactory messagesResponseFactory;

    @Mock
    private MessageAddedEventProcessor messageAddedEventProcessor;

    @Mock
    private ContentOverridingPostProcessorService contentOverridingPostProcessorService;

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
                blockUserRepository, contentOverridingPostProcessorService);

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
    }

    @Test
    public void validConvAndMsgFromBuyer_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));
        when(messagesResponseFactory.getCleanedMessage(conversation, message))
                .thenReturn("clean message");
        when(contentOverridingPostProcessorService.getCleanedMessage(conversation, message))
                .thenReturn("clean message");
        when(blockUserRepository.isBlocked(BUYER_USER_ID, SELLER_USER_ID))
                .thenReturn(false);
        when(blockUserRepository.isBlocked(SELLER_USER_ID, BUYER_USER_ID))
                .thenReturn(false);

        listener.messageProcessed(conversation, message);

        verify(blockUserRepository).isBlocked(BUYER_USER_ID, SELLER_USER_ID);
        verify(blockUserRepository).isBlocked(SELLER_USER_ID, BUYER_USER_ID);
        verify(messageAddedEventProcessor).publishMessageAddedEvent(conversation, message, "clean message", null);
        verify(userIdentifierServiceMock).getBuyerUserId(conversation);
        verify(userIdentifierServiceMock).getSellerUserId(conversation);
        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, "clean message");
        verify(delegatorMock).processNewMessage(SELLER_USER_ID, conversation, message, true, "clean message");
    }

    @Test
    public void blockedBothDirection() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));

        // Owner of the messsage
        when(blockUserRepository.isBlocked(BUYER_USER_ID, SELLER_USER_ID))
                .thenReturn(true);
        when(blockUserRepository.isBlocked(SELLER_USER_ID, BUYER_USER_ID))
                .thenReturn(true);

        UserUnreadCounts userUnreadCounts = new UserUnreadCounts(SELLER_USER_ID, 1, 1);
        when(delegatorMock.getUnreadCounts(SELLER_USER_ID))
                .thenReturn(userUnreadCounts);

        listener.messageProcessed(conversation, message);

        verify(userIdentifierServiceMock).getBuyerUserId(conversation);
        verify(userIdentifierServiceMock).getSellerUserId(conversation);
        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false ,null);
        verify(delegatorMock).getUnreadCounts(SELLER_USER_ID);
        verify(messageAddedEventProcessor).publishMessageAddedEvent(conversation, message, null, userUnreadCounts);
        verifyNoMoreInteractions(delegatorMock);
    }

    @Test
    public void recipientDirectionBlocked() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));

        // Owner of the messsage
        when(blockUserRepository.isBlocked(BUYER_USER_ID, SELLER_USER_ID))
                .thenReturn(false);
        when(blockUserRepository.isBlocked(SELLER_USER_ID, BUYER_USER_ID))
                .thenReturn(true);

        UserUnreadCounts userUnreadCounts = new UserUnreadCounts(SELLER_USER_ID, 1, 1);
        when(delegatorMock.getUnreadCounts(SELLER_USER_ID))
                .thenReturn(userUnreadCounts);

        listener.messageProcessed(conversation, message);

        verify(userIdentifierServiceMock).getBuyerUserId(conversation);
        verify(userIdentifierServiceMock).getSellerUserId(conversation);
        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false ,null);
        verify(delegatorMock).getUnreadCounts(SELLER_USER_ID);
        verify(messageAddedEventProcessor).publishMessageAddedEvent(conversation, message, null, userUnreadCounts);
        verifyNoMoreInteractions(delegatorMock);
    }

    @Test
    public void validConvAndMsgFromSeller_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withMessageDirection(SELLER_TO_BUYER).build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));

        listener.messageProcessed(conversation, message);

        verify(userIdentifierServiceMock).getBuyerUserId(conversation);
        verify(userIdentifierServiceMock).getSellerUserId(conversation);
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

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));

        listener.messageProcessed(conversation, message);

        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, null);
        verify(delegatorMock, never()).processNewMessage(SELLER_USER_ID, conversation, message, true, null);
    }

    @Test
    public void noBuyerUserId_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(empty());
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(Optional.of("SELLER_ID"));

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void noSellerUserId_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(Optional.of("BUYER_ID"));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(empty());

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void ignoredMessage_notProcessed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(IGNORED).build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(empty());
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(empty());

        listener.messageProcessed(conversation, message);

        verifyZeroInteractions(delegatorMock);
    }

    @Test
    public void blockedOwnMessage_processed() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(MessageState.BLOCKED).withMessageDirection(BUYER_TO_SELLER).build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(Optional.of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(Optional.of(SELLER_USER_ID));

        listener.messageProcessed(conversation, message);

        verify(delegatorMock).processNewMessage(BUYER_USER_ID, conversation, message, false, null);
        verify(delegatorMock, never()).processNewMessage(SELLER_USER_ID, conversation, message, true, null);
    }

    @Test
    public void exceptionDuringProcessing_propagate() {
        Conversation conversation = convBuilder.build();
        Message message = msgBuilder.withState(MessageState.BLOCKED).build();

        when(userIdentifierServiceMock.getBuyerUserId(conversation))
                .thenReturn(of(BUYER_USER_ID));
        when(userIdentifierServiceMock.getSellerUserId(conversation))
                .thenReturn(of(SELLER_USER_ID));

        doThrow(new RuntimeException("~expected exception~"))
                .when(delegatorMock)
                .processNewMessage(BUYER_USER_ID, conversation, message, false, null);

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error updating user projections for conversation cid, conversation state ACTIVE and message mid: ~expected exception~");

        listener.messageProcessed(conversation, message);
    }
}