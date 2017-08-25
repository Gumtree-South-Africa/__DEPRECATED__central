package com.ecg.replyts.core.runtime.listener;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.persistence.mail.StoredMail;
import com.google.common.base.Optional;
import kafka.common.FailedToSendMessageException;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

public class KafkaMailPublisher implements MailPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMailPublisher.class);

    private static final Counter MESSAGE_SENT_EVENT_PUBLISHED = newCounter("mail-publisher.kafka.messages-sent-successful");
    private static final Counter MESSAGE_SENT_EVENT_FAILED = newCounter("mail-publisher.kafka.messages-sent-failed");

    private final Producer<String, byte[]> producer;
    private final String topic;

    public KafkaMailPublisher(Producer<String, byte[]> producer, String topic) {
        this.topic = topic;
        this.producer = producer;
    }

    @Override
    public void publishMail(String messageId, byte[] incomingMailData, Optional<byte[]> outgoingMailData) {
        try {
            producer.send(new KeyedMessage<>(topic, messageId,
                    new StoredMail(incomingMailData, outgoingMailData).compress()));

            MESSAGE_SENT_EVENT_PUBLISHED.inc();

        } catch (FailedToSendMessageException e) {
            LOGGER.error("An error happened while trying to publish a message with id: {} to kafka", messageId, e);
            MESSAGE_SENT_EVENT_FAILED.inc();
        }
    }
}
