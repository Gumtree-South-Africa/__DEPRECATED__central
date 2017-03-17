package com.gumtree.replyts2.eventpublisher.publisher;

import com.gumtree.replyts2.eventpublisher.EventTestUtils;
import com.gumtree.replyts2.eventpublisher.event.Event;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RabbitEventPublisherTest {

    private static final String EXCHANGE_NAME = "temp";
    private static final String ROUTING_KEY = "routingKey";
    private static final String FANOUT = "fanout";

    private EventPublisher eventPublisher;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Channel channel;

    @Mock
    private Connection connection;

    @Mock
    private Event event;

    @Before
    public void setUp() throws Exception {
        eventPublisher = new RabbitEventPublisher(connectionFactory, EXCHANGE_NAME, ROUTING_KEY);
    }

    @Test
    public void testPublishEvent() throws Exception {
        String message = EventTestUtils.aMessage();
        when(event.toJsonString()).thenReturn(message);

        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        eventPublisher.publishEvent(EventTestUtils.anEvent());

//        verify(channel).exchangeDeclare(EXCHANGE_NAME, FANOUT);

        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), any(AMQP.BasicProperties.class), eq(message.getBytes()));
        verify(channel).close();
        verify(connection).close();
    }

    @Test
    public void catchesExceptionIfFailureOnConnection() throws IOException, TimeoutException {
        when(connectionFactory.newConnection()).thenThrow(IOException.class);
        verifyZeroInteractions(connection);
        verifyZeroInteractions(channel);
    }
}