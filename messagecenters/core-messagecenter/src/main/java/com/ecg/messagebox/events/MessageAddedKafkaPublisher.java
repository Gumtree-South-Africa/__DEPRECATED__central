package com.ecg.messagebox.events;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

class MessageAddedKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageAddedKafkaPublisher.class);

    private static final Counter NEW_MSG_PUBLISHED = newCounter("messageadded-publisher.sent-success");
    private static final Counter NEW_MSG_FAILED_TO_PUBLISH = newCounter("messageadded-publisher.sent-failure");

    private final Producer<String, byte[]> producer;
    private final String topic;


    MessageAddedKafkaPublisher(Producer<String, byte[]> producer, String topic) {
        this.topic = topic;
        this.producer = producer;
    }

    void publishNewMessage(EventPublisher.Event event) {
        producer.send(new ProducerRecord<>(topic, event.partitionKey, event.data), (meta, ex) -> {
            if (ex != null) {
                log.error("An error happened while trying to publish new message added event", ex);
                NEW_MSG_FAILED_TO_PUBLISH.inc();
            } else {
                NEW_MSG_PUBLISHED.inc();
            }
        });
    }
}
