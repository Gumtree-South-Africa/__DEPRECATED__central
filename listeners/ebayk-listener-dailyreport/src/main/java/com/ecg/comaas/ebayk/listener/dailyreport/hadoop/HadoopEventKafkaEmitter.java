package com.ecg.comaas.ebayk.listener.dailyreport.hadoop;

import com.ecg.comaas.ebayk.listener.dailyreport.AsyncProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class HadoopEventKafkaEmitter implements HadoopEventEmitter {
    private static final Logger LOG = LoggerFactory.getLogger(HadoopEventKafkaEmitter.class);

    private AsyncProducer<UUID, String> producer;

    public HadoopEventKafkaEmitter(AsyncProducer<UUID, String> producer) {
        this.producer = producer;
    }

    @Override
    public void insert(HadoopLogEntry entry) {
        try {
            String topicName = entry.getLogUseCase().getTopicName();
            ProducerRecord<UUID, String> record = new ProducerRecord<>(topicName, UUID.randomUUID(), entry.getLogEntry());
            producer.send(record);
            LOG.trace("Send hadoop log-entry for topic {} to Kafka", topicName);
        } catch (RuntimeException e) {
            LOG.error("Error on emitting to Kafka: hadoop log-entry {}", entry.getLogEntry(), e);
        }
    }
}

