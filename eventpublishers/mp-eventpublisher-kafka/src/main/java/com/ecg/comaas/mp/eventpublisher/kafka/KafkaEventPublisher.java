package com.ecg.comaas.mp.eventpublisher.kafka;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KafkaEventPublisher implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private static final Counter CONVERSATION_EVENT_PUBLISHED = TimingReports.newCounter("event-publisher.kafka.messages-sent");
    private static final Counter CONVERSATION_EVENT_FAILED = TimingReports.newCounter("event-publisher.kafka.messages-sent-failed");
    private static final Counter USER_EVENT_PUBLISHED = TimingReports.newCounter("event-publisher.kafka.user.event.messages-sent");
    private static final Counter USER_EVENT_FAILED = TimingReports.newCounter("event-publisher.kafka.user.event.messages-sent-failed");

    private final KafkaProducer<String, byte[]> producer;
    private final String conversationEventsTopic;
    private final String userEventsTopic;


    public KafkaEventPublisher(KafkaProducer<String, byte[]> producer, String conversationEventsTopic, String userEventsTopic) {
        this.conversationEventsTopic = conversationEventsTopic;
        this.producer = producer;
        this.userEventsTopic = userEventsTopic;
    }

    @Override
    public void publishConversationEvents(List<Event> events) {
        try {
            sendMessage(events, conversationEventsTopic);

            CONVERSATION_EVENT_PUBLISHED.inc(events.size());

        } catch (KafkaException e) {
            LOGGER.error("An error happened while trying to send a message: {}", e.getMessage());
            CONVERSATION_EVENT_FAILED.inc();
        }
    }

    @Override
    public void publishUserEvents(List<Event> events) {
        try {
            sendMessage(events, userEventsTopic);

            USER_EVENT_PUBLISHED.inc(events.size());

        } catch (KafkaException e) {
            LOGGER.error("An error happened while trying to send a message: {}", e.getMessage());
            USER_EVENT_FAILED.inc();
        }
    }

    private void sendMessage(List<Event> events, String topic) {
        events.forEach(event -> producer.send(new ProducerRecord<>(topic, event.partitionKey, event.data)));
    }
}
