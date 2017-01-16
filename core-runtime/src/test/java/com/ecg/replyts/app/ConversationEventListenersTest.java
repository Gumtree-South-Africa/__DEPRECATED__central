package com.ecg.replyts.app;

import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.CustomValueAddedEvent;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConversationEventListenersTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConversationEventListenersTest {
    @Autowired
    private List<ConversationEventListener> listeners;

    @Autowired
    private ConversationEventListeners eventListeners;

    @Mock
    private ImmutableConversation conversation;

    private List<ConversationEvent> events = Lists.newArrayList(new CustomValueAddedEvent("foo", "bar"));

    @Test
    public void testSingleListenerIsCalled() throws Exception {
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);

        listeners.add(conversationEventListener);

        eventListeners.processEventListeners(conversation, events);

        verify(conversationEventListener).eventsTriggered(conversation, events);
    }

    @Test
    public void testListenersAreCalledInOrder() throws Exception {
        ConversationEventListener listener1 = mock(ConversationEventListener.class);
        when(listener1.getOrder()).thenReturn(10);
        listeners.add(listener1);
        ConversationEventListener listener2 = mock(ConversationEventListener.class);
        when(listener2.getOrder()).thenReturn(5);
        listeners.add(listener2);

        eventListeners.sortListeners();
        eventListeners.processEventListeners(conversation, events);

        // listener2 is called before listener1:
        InOrder inOrder = inOrder(listener1, listener2);
        inOrder.verify(listener2).eventsTriggered(conversation, events);
        inOrder.verify(listener1).eventsTriggered(conversation, events);
    }

    @Test
    public void testListenerWithException() throws Exception {
        ConversationEventListener conversationEventListener = mock(ConversationEventListener.class);
        listeners.add(conversationEventListener);

        doThrow(new RuntimeException("foo")).when(conversationEventListener).eventsTriggered(conversation, events);

        // Should not rethrow the exception:
        eventListeners.processEventListeners(conversation, events);
    }

    @Test
    public void testProcessEventListenersWithoutListener() throws Exception {
         eventListeners.processEventListeners(conversation, events);
    }

    @Configuration
    @Import(ConversationEventListeners.class)
    static class TestContext {
        @Bean
        public List<ConversationEventListener> listeners() {
            return new ArrayList<>();
        }
    }
}
