package com.ecg.replyts.core.runtime.persistence.kafka;

import com.google.protobuf.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

@Service
public class QueueService {
    private static final Logger LOG = LoggerFactory.getLogger(QueueService.class);

    @Value("${kafka.core.servers}")
    private String bootstrapServers;

    private Map<String, Producer<String, byte[]>> producers = new ConcurrentHashMap<>();

    @PreDestroy
    void destroy() {
        producers.values().forEach(producer -> {
            try {
                producer.close();
            } catch (Exception e) {
                LOG.warn("Caught exception when trying to close Kafka message processing producer", e);
            }
        });
    }

    public void publishSynchronously(String topicName, Message message) {
        publishSynchronously(topicName, null, message.toByteArray());
    }

    public void publishSynchronously(String topicName, @Nullable String key, Message message) {
        publishSynchronously(topicName, key, message.toByteArray());
    }

    public void publishSynchronously(String topicName, byte[] payload) {
        publishSynchronously(topicName, null, payload);
    }

    private void publishSynchronously(String topicName, String key, byte[] payload) {
        Producer<String, byte[]> producer = producers.computeIfAbsent(topicName, topic -> newProducer());
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicName, null, key, payload);

        try {
            RecordMetadata metadata = producer.send(record).get();
            LOG.trace("record metadata: {}", metadata.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("interrupted while waiting for the producer to send a record");
        } catch (ExecutionException e) {
            throw new RuntimeException("failed to send a kafka record", e);
        }
    }

    private Producer<String, byte[]> newProducer() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        String allocId = System.getenv("NOMAD_ALLOC_ID");
        if (allocId != null) {
            long threadId = Thread.currentThread().getId();
            props.put(ProducerConfig.CLIENT_ID_CONFIG, format("%s-%d", allocId, threadId));
        }
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        return new KafkaProducer<>(props);
    }
}
