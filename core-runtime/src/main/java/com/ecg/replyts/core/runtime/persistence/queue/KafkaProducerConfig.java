package com.ecg.replyts.core.runtime.persistence.queue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

public class KafkaProducerConfig<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerConfig.class);


    @Value("${kafka.attachment.servers:replyts.dev.kjdev.ca:9092}")
    private String servers;

    @Value("${kafka.attachment.key.serializer:org.apache.kafka.common.serialization.StringSerializer}")
    private String keySerializer;

    @Value("${kafka.attachment.value.serializer:org.apache.kafka.common.serialization.ByteArraySerializer}")
    private String valueSerializer;

    @Value("${kafka.attachment.compressionType:none}")
    private String compressionType;

    @Value("${kafka.attachment.retries:1}")
    private int retries;

    // This parameter controls the amount of memory in bytes (not messages!) that will be used for each batch.
    @Value("${kafka.attachment.batch.size:16500000}") // 5 MB
    private int batchSize;

    // The acks parameter controls how many partition replicas must receive the record before the producer can consider the write successful.
    @Value("${kafka.attachment.ack:1}")
    private String ack;

    @Value("${kafka.max.request.size:16000000}")
    private int maxRequestSize; // 15.5 MB

    @Value("${kafka.attachment.max-in-flight-request-per-connection:10}")
    private int maxInFlightPerConnection;

    private KafkaProducer<K, V> producer;

    @PostConstruct
    private void createKafkaProducer() {
        Properties configProperties = new Properties();
        LOG.info("Connecting to Kafka {} for attachment storage ", servers);
        configProperties.put(BOOTSTRAP_SERVERS_CONFIG, servers);
        configProperties.put(KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProperties.put(VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        configProperties.put(COMPRESSION_TYPE_CONFIG, compressionType);
        configProperties.put(RETRIES_CONFIG, retries);
        configProperties.put(BATCH_SIZE_CONFIG, batchSize);
        configProperties.put(ACKS_CONFIG, ack);
        configProperties.put(MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        configProperties.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightPerConnection);
        producer = new KafkaProducer<>(configProperties);
        LOG.info("Kafka producer configuration: {} ", configProperties);
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
        producer = null;
    }

    public KafkaProducer<K, V> getProducer() {
        return producer;
    }
}