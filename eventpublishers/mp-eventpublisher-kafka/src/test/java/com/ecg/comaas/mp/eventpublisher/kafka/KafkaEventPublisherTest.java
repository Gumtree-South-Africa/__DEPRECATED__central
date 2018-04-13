package com.ecg.comaas.mp.eventpublisher.kafka;

import com.ecg.comaas.mp.eventpublisher.kafka.KafkaEventPublisher;
import com.ecg.replyts.app.eventpublisher.EventPublisher.Event;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaEventPublisherTest {

    private final byte[] data1 = new byte[]{0x01, 0x00, 0x00};
    private final byte[] data2 = new byte[]{0x02, 0x00, 0x00};

    private KafkaEventPublisher eventPublisher;

    @Mock
    private KafkaProducer<String, byte[]> producer;
    @Captor
    private ArgumentCaptor<ProducerRecord<String, byte[]>> producedMessagesCaptor;

    @Before
    public void setUp() throws Exception {
        eventPublisher = new KafkaEventPublisher(producer, "topic1", "topic2");
    }

    @Test
    public void testPublishConversationEvents() throws Exception {
        List<Event> events = Arrays.asList(
                new Event("p1", data1),
                new Event("p2", data2)
        );

        eventPublisher.publishConversationEvents(events);

        verify(producer, times(2)).send(producedMessagesCaptor.capture());
        List<ProducerRecord<String, byte[]>> producedMessages = producedMessagesCaptor.getAllValues();
        assertThat(producedMessages.size(), is(2));
        assertThat(producedMessages.get(0).topic(), is("topic1"));
        assertThat(producedMessages.get(0).key(), is("p1"));
        assertThat(producedMessages.get(0).value(), sameInstance(data1));
        assertThat(producedMessages.get(1).topic(), is("topic1"));
        assertThat(producedMessages.get(1).key(), is("p2"));
        assertThat(producedMessages.get(1).value(), sameInstance(data2));
    }

    @Test
    public void testPublishUserEvents() throws Exception {
        List<Event> events = Arrays.asList(
                new Event("p1", data1),
                new Event("p2", data2)
        );

        eventPublisher.publishUserEvents(events);

        verify(producer, times(2)).send(producedMessagesCaptor.capture());
        List<ProducerRecord<String, byte[]>> producedMessages = producedMessagesCaptor.getAllValues();
        assertThat(producedMessages.size(), is(2));
        assertThat(producedMessages.get(0).topic(), is("topic2"));
        assertThat(producedMessages.get(0).key(), is("p1"));
        assertThat(producedMessages.get(0).value(), sameInstance(data1));
        assertThat(producedMessages.get(1).topic(), is("topic2"));
        assertThat(producedMessages.get(1).key(), is("p2"));
        assertThat(producedMessages.get(1).value(), sameInstance(data2));
    }
}