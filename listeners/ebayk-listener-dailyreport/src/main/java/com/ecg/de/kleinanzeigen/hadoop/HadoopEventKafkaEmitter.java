package com.ecg.de.kleinanzeigen.hadoop;

import com.ecg.de.kleinanzeigen.AsyncProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by johndavis on 29/11/16.
 */
public class HadoopEventKafkaEmitter implements HadoopEventEmitter {
    private static final Logger LOG = LoggerFactory.getLogger(HadoopEventKafkaEmitter.class);

    @Resource(name = "kafkaUUIDProducer")
    private AsyncProducer<UUID, String> producer;

    HadoopEventKafkaEmitter() {
    }

    @Override
    public void insert(HadoopLogEntry entry) {
        try {
            String topicName = entry.getLogUseCase().getTopicName();
            ProducerRecord<UUID, String> record = new ProducerRecord<>(topicName, UUID.randomUUID(), entry.getLogEntry());
            producer.send(record);
            LOG.debug("Send hadoop log-entry for topic {} to Kafka", topicName);
        } catch (RuntimeException e) {
            LOG.error("Error on emitting to Kafka: hadoop log-entry {}", entry.getLogEntry(), e);
        }
    }
}

