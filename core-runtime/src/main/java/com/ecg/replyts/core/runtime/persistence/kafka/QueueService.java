package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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

    public void publish(final String topicName, final Message retryableMessage) {
        publish(topicName, serialize(retryableMessage));
    }

    public void publish(final String topicName, final byte[] payload) {
        final Producer<String, byte[]> producer = producers.computeIfAbsent(topicName, this::producer);
        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicName, payload);
        producer.send(record);
        producer.flush();
    }

    private Producer<String, byte[]> producer(final String topic) {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer_" + topic);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    private byte[] serialize(final Message retryableMessage) {
        return retryableMessage.toByteArray();
    }

    public Message deserialize(final byte[] data) throws IOException {
        return Message.parseFrom(data);
    }
}
