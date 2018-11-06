package com.ecg.replyts.core.runtime.configadmin;

import ecg.unicom.events.configuration.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ConfigurationPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationPublisher.class);

    private final String topicName;
    private final String tenant;
    private final KafkaProducer<String, byte[]> kafkaProducer;

    public ConfigurationPublisher(Properties properties, String topicName, String tenant) {
        this.kafkaProducer = new KafkaProducer<>(properties);
        this.topicName = topicName;
        this.tenant = tenant;
    }

    public void publish(Configuration.Envelope event) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicName, tenant, event.toByteArray());

        kafkaProducer.send(record, (metadata, exception) -> {
            if (metadata == null) {
                LOG.error("Error while sending event to Kafka", exception);
            }
        });
    }

    public void close() {
        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
