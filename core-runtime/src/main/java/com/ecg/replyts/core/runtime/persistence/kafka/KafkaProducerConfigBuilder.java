package com.ecg.replyts.core.runtime.persistence.kafka;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Component
@Scope(value = "prototype")
public class KafkaProducerConfigBuilder<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerConfigBuilder.class);

    @Value("${kafka.core.servers:localhost:9092}")
    private String servers;

    @Value("${kafka.core.key.serializer:org.apache.kafka.common.serialization.StringSerializer}")
    private String keySerializer;

    @Value("${kafka.core.value.serializer:org.apache.kafka.common.serialization.ByteArraySerializer}")
    private String valueSerializer;

    @Value("${kafka.core.compressionType:none}")
    private String compressionType;

    @Value("${kafka.core.retries:1}")
    private int retries;

    // This parameter controls the amount of memory in bytes (not messages!) that will be used for each batch.
    @Value("${kafka.core.batch.size:5000000}")
    private int batchSize;

    // The acks parameter controls how many partition replicas must receive the record before the producer can consider the write successful.
    @Value("${kafka.core.ack:1}")
    private String ack;

    @Value("${kafka.core.max.request.size:1000000}") // 1 mb
    private int maxRequestSize;

    @Value("${kafka.core.max-in-flight-request-per-connection:10}")
    private int maxInFlightPerConnection;

    @Value("${kafka.core.request.timeout.ms:10000}")
    private int storeTimeoutMs;

    private String topic;

    private final KafkaProducerConfig pconfig = new KafkaProducerConfig();

    public KafkaProducerConfig getProducerConfig() {
        return pconfig;
    }

    public class KafkaProducerConfig<K, V> {

        KafkaProducer<K, V> producer;

        public KafkaProducer<K, V> getProducer() {
            return producer;
        }

        @PreDestroy
        public void close() {
            try {
                if (producer != null) producer.close(4000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOG.error("Failed to close Kafka producer", e);
            }
            producer = null;
        }

        public String getTopic() {
            return topic;
        }

        public KafkaProducerConfig withTopic(String topicVal) {
            topic = topicVal;
            return this;
        }

        public int getStoreTimeoutMs() {
            return storeTimeoutMs;
        }

        public KafkaProducerConfig withServers(String serversVal) {
            servers = serversVal;
            return this;
        }

        public KafkaProducerConfig withKeySerializer(String keySerializerVal) {
            keySerializer = keySerializerVal;
            return this;
        }

        public KafkaProducerConfig withValueSerializer(String valueSerializerVal) {
            valueSerializer = valueSerializerVal;
            return this;
        }

        public KafkaProducerConfig withCompressionType(String compressionTypeVal) {
            compressionType = compressionTypeVal;
            return this;
        }

        public KafkaProducerConfig withRetries(int retriesVal) {
            retries = retriesVal;
            return this;
        }

        public KafkaProducerConfig withBatchSize(int batchSizeVal) {
            batchSize = batchSizeVal;
            return this;
        }

        public KafkaProducerConfig withAck(String ackVal) {
            ack = ackVal;
            return this;
        }

        public KafkaProducerConfig withMaxRequestSize(int maxRequestSizeVal) {
            maxRequestSize = maxRequestSizeVal;
            return this;
        }

        public KafkaProducerConfig withStoreTimeoutMs(int storeTimeoutMsVal) {
            storeTimeoutMs = storeTimeoutMsVal;
            return this;
        }

        public KafkaProducerConfig withMaxInFlightPerConnection(int maxInFlightPerConnectionVal) {
            maxInFlightPerConnection = maxInFlightPerConnectionVal;
            return this;
        }

        public KafkaProducerConfig build() {
            Preconditions.checkState(StringUtils.isNotBlank(topic), "Kafka topic must be specified");
            Properties configProperties = new Properties();
            LOG.info("Connecting to Kafka {} ", servers);
            configProperties.put(BOOTSTRAP_SERVERS_CONFIG, servers);
            configProperties.put(KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
            configProperties.put(VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
            configProperties.put(COMPRESSION_TYPE_CONFIG, compressionType);
            configProperties.put(RETRIES_CONFIG, retries);
            configProperties.put(BATCH_SIZE_CONFIG, batchSize);
            configProperties.put(ACKS_CONFIG, ack);
            configProperties.put(MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
            configProperties.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightPerConnection);
            LOG.info("Kafka producer configuration: {} Topic: {}, Timeout: {} ms", configProperties, topic, storeTimeoutMs);
            producer = new KafkaProducer<>(configProperties);
            return this;
        }

        public String getKeySerializer() {
            return keySerializer;
        }

        public String getValueSerializer() {
            return valueSerializer;
        }

        public String getCompressionType() {
            return compressionType;
        }

        public int getRetries() {
            return retries;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public String getAck() {
            return ack;
        }

        public int getMaxRequestSize() {
            return maxRequestSize;
        }

        public int getMaxInFlightPerConnection() {
            return maxInFlightPerConnection;
        }

        public String getServers() {
            return servers;
        }
    }

}