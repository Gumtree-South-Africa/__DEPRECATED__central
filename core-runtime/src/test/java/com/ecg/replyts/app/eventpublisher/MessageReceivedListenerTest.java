package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageReceivedListenerTest {

    private MessageReceivedListener messageReceivedListener;
    private byte[] jsonBytes1 = { 0x01, 0x00, 0x00 };
    private byte[] jsonBytes2 = { 0x02, 0x00, 0x00 };
    private byte[] jsonBytes3 = { 0x03, 0x00, 0x00 };

    @Mock
    private Conversation conversation;
    @Mock
    private MailCloakingService mailCloakingService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private EventSerializer serializer;
    @Captor
    private ArgumentCaptor<ExtendedConversationEvent> serializerEventCaptor;
    @Captor
    private ArgumentCaptor<List<EventPublisher.Event>> publisherEventsCaptor;

    @Test
    public void sendsEvents() throws Exception {
        when(conversation.getId()).thenReturn("conversation-id");
        when(conversation.getAdId()).thenReturn("m87963254");
        when(conversation.getState()).thenReturn(ConversationState.ACTIVE);

        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress("anon-seller@a.com"));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("anon-buyer@a.com"));
        when(serializer.serialize(any())).thenReturn(jsonBytes1, jsonBytes2, jsonBytes3);

        List<ConversationEvent> conversationEvents = ImmutableList.of(
                EventTestUtils.messageAddedEvent(),
                EventTestUtils.messageAddedEventWithMessageInBlockedState(),
                EventTestUtils.messageFilteredEvent());

        messageReceivedListener = new MessageReceivedListener(new EventConverter(mailCloakingService, serializer), eventPublisher);
        messageReceivedListener.eventsTriggered(conversation, conversationEvents);

        verify(serializer, times(3)).serialize(serializerEventCaptor.capture());
        List<ExtendedConversationEvent> serializedEvents = serializerEventCaptor.getAllValues();
        assertThat(serializedEvents.size(), is(3));
        for (ExtendedConversationEvent serializedEvent : serializedEvents) {
            assertThat(serializedEvent.conversation.sellerAnonymousEmail, is("anon-seller@a.com"));
            assertThat(serializedEvent.conversation.buyerAnonymousEmail, is("anon-buyer@a.com"));
        }

        verify(eventPublisher).publishConversationEvents(publisherEventsCaptor.capture());
        List<EventPublisher.Event> events = publisherEventsCaptor.getValue();
        assertThat(events.size(), is(3));
        assertThat(events.get(0).data, sameInstance(jsonBytes1));
        assertThat(events.get(1).data, sameInstance(jsonBytes2));
        assertThat(events.get(2).data, sameInstance(jsonBytes3));
        assertThat(events.get(0).partitionKey, is("conversation-id"));
        assertThat(events.get(1).partitionKey, is("conversation-id"));
        assertThat(events.get(2).partitionKey, is("conversation-id"));
    }

}