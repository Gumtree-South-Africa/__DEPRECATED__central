package com.ecg.replyts.app;

import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.CustomValueAddedEvent;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

public class ConversationEventListenersTest {

    private ImmutableConversation conversation = mock(ImmutableConversation.class);
    private ConversationEvent conversationEvent = new CustomValueAddedEvent("foo", "bar");
    private List<ConversationEvent> events = Lists.newArrayList(conversationEvent);

    @Test
    public void testSingleListenerIsCalled() throws Exception {
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);
        List<ConversationEventListener> eventListeners = Lists.newArrayList(conversationEventListener);

        new ConversationEventListeners(eventListeners).processEventListeners(conversation, events);

        verify(conversationEventListener).eventsTriggered(conversation, events);
    }

    @Test
    public void testListenersAreCalledInOrder() throws Exception {
        ConversationEventListener listener1 = mock(ConversationEventListener.class);
        when(listener1.getOrder()).thenReturn(10);
        ConversationEventListener listener2 = mock(ConversationEventListener.class);
        when(listener2.getOrder()).thenReturn(5);

        List<ConversationEventListener> eventListeners = Lists.newArrayList(listener1, listener2);

        new ConversationEventListeners(eventListeners).processEventListeners(conversation, events);

        // listener2 is called before listener1:
        InOrder inOrder = inOrder(listener1, listener2);
        inOrder.verify(listener2).eventsTriggered(conversation, events);
        inOrder.verify(listener1).eventsTriggered(conversation, events);
    }

    @Test
    public void testListenerWithException() throws Exception {
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);
        List<ConversationEventListener> eventListeners = Lists.newArrayList(conversationEventListener);

        doThrow(new RuntimeException("foo")).when(conversationEventListener).eventsTriggered(conversation, events);

        // Should not rethrow the exception:
        new ConversationEventListeners(eventListeners).processEventListeners(conversation, events);
    }

    @Test
    public void testProcessEventListenersWithoutListener() throws Exception {
         new ConversationEventListeners().processEventListeners(conversation, events);
    }
}
