package com.ecg.gumtree.comaas.filter.volume;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static com.ecg.gumtree.comaas.filter.volume.SharedBrain.GUMTREE_VELOCITY_FILTER_EXCHANGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SharedBrainTest {
    private static final String NEW_MESSAGE_LISTENER_ID = "message-listener-id-new";

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private EventStreamProcessor eventStreamProcessor;

    @Mock
    private ITopic<String> communicationBus;

    @SuppressWarnings("unchecked")
    @Test
    public void addMessageListener() {
        when(communicationBus.addMessageListener(any())).thenReturn(NEW_MESSAGE_LISTENER_ID);
        when(hazelcastInstance.<String>getTopic(GUMTREE_VELOCITY_FILTER_EXCHANGE)).thenReturn(communicationBus);

        SharedBrain sharedBrain = new SharedBrain(hazelcastInstance, eventStreamProcessor);
        AtomicReference<String> messageListenerId = (AtomicReference<String>) ReflectionTestUtils.getField(sharedBrain, "messageListenerId");
        verify(communicationBus, times(1)).addMessageListener(any(MessageListener.class));
        verify(communicationBus, never()).removeMessageListener(eq("default"));
        assertThat(messageListenerId.get(), equalTo(NEW_MESSAGE_LISTENER_ID));

        when(communicationBus.addMessageListener(any(MessageListener.class))).thenReturn("newer-message-listener-id");
        sharedBrain = new SharedBrain(hazelcastInstance, eventStreamProcessor);

        AtomicReference<String> messageListenerId1 = (AtomicReference<String>) ReflectionTestUtils.getField(sharedBrain, "messageListenerId");

        verify(communicationBus, times(2)).addMessageListener(any(MessageListener.class));
        verify(communicationBus, times(1)).removeMessageListener(NEW_MESSAGE_LISTENER_ID);
        assertThat(messageListenerId1.get(), equalTo("newer-message-listener-id"));
    }
}