package com.ecg.replyts.core.runtime.listener;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.persistence.mail.StoredMail;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

public class KafkaMailPublisher implements MailPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMailPublisher.class);

    private static final Counter MESSAGE_SENT_EVENT_PUBLISHED = newCounter("mail-publisher.kafka.messages-sent-successful");
    private static final Counter MESSAGE_SENT_EVENT_FAILED = newCounter("mail-publisher.kafka.messages-sent-failed");

    private final KafkaProducer<String, byte[]> producer;
    private final String topic;

    public KafkaMailPublisher(KafkaProducer<String, byte[]> producer, String topic) {
        this.topic = topic;
        this.producer = producer;
    }

    @Override
    public void publishMail(String messageId, byte[] incomingMailData, Optional<byte[]> outgoingMailData) {
        try {
            producer.send(new ProducerRecord<>(topic, messageId,
                    new StoredMail(incomingMailData, outgoingMailData).compress()));

            MESSAGE_SENT_EVENT_PUBLISHED.inc();
            LOGGER.trace("Message {} sent to Kafka topic {}", messageId, topic);

        } catch (KafkaException e) {
            LOGGER.error("An error happened while trying to publish a message with id: {} to kafka", messageId, e);
            MESSAGE_SENT_EVENT_FAILED.inc();
        }
    }
}
