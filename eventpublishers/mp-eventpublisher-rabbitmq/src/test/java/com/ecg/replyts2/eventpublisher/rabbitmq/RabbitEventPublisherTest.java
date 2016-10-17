package com.ecg.replyts2.eventpublisher.rabbitmq;

import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.rabbitmq.client.Channel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RabbitEventPublisherTest {
    private final byte[] data1 = new byte[]{0x01, 0x00, 0x00};
    private final byte[] data2 = new byte[]{0x02, 0x00, 0x00};

    private RabbitEventPublisher eventPublisher;

    @Mock
    private Channel channel;
    @Captor
    private ArgumentCaptor<byte[]> publishedMessagesCaptor;

    @Before
    public void setUp() {
        eventPublisher = new RabbitEventPublisher(channel, "exchangeName1", "routingKey1");
    }

    @Test
    public void testPublishEvents() throws Exception {
        List<EventPublisher.Event> events = Arrays.asList(
                new EventPublisher.Event("p1", data1),
                new EventPublisher.Event("p2", data2)
        );

        eventPublisher.publishConversationEvents(events);

        verify(channel, times(2)).basicPublish(eq("exchangeName1"), eq("routingKey1"), eq(null), publishedMessagesCaptor.capture());
        List<byte[]> publishedMessages = publishedMessagesCaptor.getAllValues();
        assertThat(publishedMessages.size(), is(2));
        assertThat(publishedMessages.get(0), sameInstance(data1));
        assertThat(publishedMessages.get(1), sameInstance(data2));
    }

}