package com.ecg.replyts2.eventpublisher.kafka;

import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.app.eventpublisher.EventPublisher;

import kafka.common.FailedToSendMessageException;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import com.codahale.metrics.Counter;

public class KafkaEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private static final Counter MESSAGE_SENT_EVENT_PUBLISHED = TimingReports.newCounter("event-publisher.kafka.messages-sent");
    private static final Counter MESSAGE_SENT_EVENT_FAILED = TimingReports.newCounter("event-publisher.kafka.messages-sent-failed");

    private final Producer<String, byte[]> producer;
    private final String topic;

    public KafkaEventPublisher(Producer<String, byte[]> producer, String topic) {
        this.topic = topic;
        this.producer = producer;
    }

    @Override
    public void publishEvents(List<Event> events) {
        try {
            List<KeyedMessage<String, byte[]>> keyedMessages = events
                    .stream()
                    .map(event -> new KeyedMessage<>(topic, event.partitionKey, event.data))
                    .collect(Collectors.toList());
            producer.send(keyedMessages);

            MESSAGE_SENT_EVENT_PUBLISHED.inc(events.size());

        } catch (FailedToSendMessageException e) {
            LOGGER.error(String.format("An error happened while trying to send a message: %s", e.getMessage()));
            MESSAGE_SENT_EVENT_FAILED.inc();
        }
    }
}
