package com.gumtree.replyts2.eventpublisher.publisher;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import com.gumtree.replyts2.eventpublisher.event.Event;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitEventPublisher implements EventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private static final Counter MESSAGE_SENT_EVENT_PUBLISHED = TimingReports.newCounter("event-publisher.message-sent-event-published");
    private static final Counter MESSAGE_SENT_EVENT_FAILED = TimingReports.newCounter("event-publisher.message-sent-event-failed");

    private String exchangeName;
    private ConnectionFactory connectionFactory;
    private String routingKey;

    @Autowired
    public RabbitEventPublisher(ConnectionFactory connectionFactory, String exchangeName, String routingKey) {
        this.connectionFactory = connectionFactory;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publishEvent(Event event) {
        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            AMQP.BasicProperties properties = createPublishProperties(event);
            channel.basicPublish(exchangeName, routingKey, properties, event.toJsonString().getBytes());
            if (LOG.isTraceEnabled()) {
                LOG.trace(" Event Sent:\n{} ", event.getEventLoggerFriendly());
            }
            MESSAGE_SENT_EVENT_PUBLISHED.inc();

            cleanup(connection, channel);
        } catch (IOException e) {
            LOG.error("An error happened when creating the connection: {}", e.getMessage());
            MESSAGE_SENT_EVENT_FAILED.inc();
        } catch (TimeoutException e) {
            LOG.error("Could not create the connection in a reasonable time: {}", e.getMessage());
            MESSAGE_SENT_EVENT_FAILED.inc();
        }
    }

    private AMQP.BasicProperties createPublishProperties(Event event) {
        Map<String, Object> headers = ImmutableMap.of("EventName", event.getEventName());
        return new AMQP.BasicProperties.Builder().headers(headers).build();
    }

    private void cleanup(Connection connection, Channel channel) throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
