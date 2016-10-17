package com.ecg.replyts2.eventpublisher.rabbitmq;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.runtime.TimingReports;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class RabbitEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private static final Counter MESSAGE_SENT_EVENT_PUBLISHED = TimingReports.newCounter("event-publisher.rabbitmq.messages-sent");
    private static final Counter MESSAGE_SENT_EVENT_FAILED = TimingReports.newCounter("event-publisher.rabbitmq.messages-sent-failed");

    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;

    public RabbitEventPublisher(Channel channel, String exchangeName, String routingKey) {
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publishConversationEvents(List<Event> events) {
        events.forEach(this::publishEvent);
    }

    @Override
    public void publishUserEvents(List<Event> events) {

    }

    private void publishEvent(Event event) {
        try {
            channel.basicPublish(exchangeName, routingKey, null, event.data);
            MESSAGE_SENT_EVENT_PUBLISHED.inc();

        } catch (IOException e) {
            LOGGER.error(String.format("An error happened when creating the connection: %s", e.getMessage()));
            MESSAGE_SENT_EVENT_FAILED.inc();
        }
    }

}
