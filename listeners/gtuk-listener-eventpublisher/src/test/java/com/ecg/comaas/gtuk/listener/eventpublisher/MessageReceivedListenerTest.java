package com.ecg.comaas.gtuk.listener.eventpublisher;

import com.ecg.comaas.gtuk.listener.eventpublisher.event.EventTestUtils;
import com.ecg.comaas.gtuk.listener.eventpublisher.publisher.EventPublisher;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageReceivedListenerTest {

    private MessageReceivedListener messageReceivedListener;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Message message;

    @Mock
    private Conversation conversation;


    @Test
    public void sendsEventIfTheMessageHasBeenSent() throws Exception {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getPlainTextBody()).thenReturn(EventTestUtils.aRawEmail());
        when(conversation.getAdId()).thenReturn("1");
        when(conversation.getId()).thenReturn("2D4");
        when(conversation.getBuyerId()).thenReturn("Buyer");
        when(conversation.getSellerId()).thenReturn("Seller");

        messageReceivedListener = new MessageReceivedListener(eventPublisher, true);
        messageReceivedListener.messageProcessed(conversation, message);

        verify(eventPublisher).publishEvent(EventTestUtils.anEvent());
    }

    @Test
    public void doesNotSendEventIfTheMessageHasBeenBlocked() {
        when(message.getState()).thenReturn(MessageState.BLOCKED);
        messageReceivedListener = new MessageReceivedListener(eventPublisher, true);
        messageReceivedListener.messageProcessed(conversation, message);
        verifyZeroInteractions(eventPublisher);
    }

    @Test
    public void doesNotSendEventIfThePublisherDisabled() {
        when(message.getState()).thenReturn(MessageState.BLOCKED);
        messageReceivedListener = new MessageReceivedListener(eventPublisher, false);
        messageReceivedListener.messageProcessed(conversation, message);
        verifyZeroInteractions(eventPublisher);
    }
}