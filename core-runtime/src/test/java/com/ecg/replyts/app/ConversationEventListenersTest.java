package com.ecg.replyts.app;

import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.CustomValueAddedEvent;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class ConversationEventListenersTest {


    @Test
    public void testProcessEventListenersWithOneListener() throws Exception {

        ImmutableConversation conversation = mock(ImmutableConversation.class);
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);

        List<ConversationEventListener> eventListeners = Lists.newArrayList(conversationEventListener);

        ConversationEvent conversationEvent = new CustomValueAddedEvent("foo", "bar");

        List<ConversationEvent> events = Lists.newArrayList(conversationEvent);

        new ConversationEventListeners(eventListeners).processEventListeners(conversation, events);

        verify(conversationEventListener).eventsTriggered(conversation, events);
    }

    @Test
    public void testListenerWithException() throws Exception {

        ImmutableConversation conversation = mock(ImmutableConversation.class);
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);

        List<ConversationEventListener> eventListeners = Lists.newArrayList(conversationEventListener);

        ConversationEvent conversationEvent = new CustomValueAddedEvent("foo", "bar");

        List<ConversationEvent> events = Lists.newArrayList(conversationEvent);

        doThrow(new RuntimeException("foo")).when(conversationEventListener).eventsTriggered(conversation, events);

        new ConversationEventListeners(eventListeners).processEventListeners(conversation, events);
    }

    @Test
    public void testProcessEventListenersWithoutListener() throws Exception {
        ImmutableConversation conversation = mock(ImmutableConversation.class);

        ConversationEvent conversationEvent = new CustomValueAddedEvent("foo", "bar");

        List<ConversationEvent> events = Lists.newArrayList(conversationEvent);

        new ConversationEventListeners().processEventListeners(conversation, events);

    }

}