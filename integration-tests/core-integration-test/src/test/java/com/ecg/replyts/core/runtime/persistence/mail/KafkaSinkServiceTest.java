package com.ecg.replyts.core.runtime.persistence.mail;


import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentKafkaConsumerConfig;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaProducerConfigBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = KafkaSinkServiceTest.TestContext.class)
public class KafkaSinkServiceTest {

    static int attStoreTimeoutMs = 500;
    static int attRetries = 10;
    static int attMaxInFlightPerConnection = 99;
    static int attMaxRequestSize = 119;
    static int attBatchSize = 3333;
    static String attTopic = "attachments";
    static String attAck = "-1";
    static String attCompressionType = "snappy";
    static String attKeySerializer = "org.apache.kafka.common.serialization.StringSerializer";
    static String attServers = "k1:9092,k2:9092";
    static String attValueSerializer = "org.apache.kafka.common.serialization.ByteArraySerializer";

    static int esStoreTimeoutMs = 600;
    static int esRetries = 7;
    static int esMaxInFlightPerConnection = 88;
    static int esMaxRequestSize = 11;
    static int esBatchSize = 2222;
    static String esTopic = "esdocuments";
    static String esAck = "all";
    static String esCompressionType = "none";
    static String esKeySerializer = "org.apache.kafka.common.serialization.StringSerializer";
    static String esServers = "f1:9092,f2:9092";
    static String esValueSerializer = "org.apache.kafka.common.serialization.ByteArraySerializer";

    @Autowired
    @Qualifier("attachmentConfig")
    private KafkaProducerConfigBuilder.KafkaProducerConfig attConfig;

    @Autowired
    @Qualifier("esConfig")
    private KafkaProducerConfigBuilder.KafkaProducerConfig esConfig;

    @Autowired
    @Qualifier("defaultConfig")
    private KafkaProducerConfigBuilder.KafkaProducerConfig defaultConfig;

    @Test
    public void producerBuilders() {

        assertEquals(attTopic, attConfig.getTopic());
        assertEquals(attServers, attConfig.getServers());
        assertEquals(attRetries, attConfig.getRetries());
        assertEquals(attMaxRequestSize, attConfig.getMaxRequestSize());
        assertEquals(attStoreTimeoutMs, attConfig.getStoreTimeoutMs());
        assertEquals(attBatchSize, attConfig.getBatchSize());
        assertEquals(attMaxInFlightPerConnection, attConfig.getMaxInFlightPerConnection());
        assertEquals(attAck, attConfig.getAck());
        assertEquals(attCompressionType, attConfig.getCompressionType());
        assertEquals(attKeySerializer, attConfig.getKeySerializer());
        assertEquals(attValueSerializer, attConfig.getValueSerializer());

        assertEquals(esTopic, esConfig.getTopic());
        assertEquals(esServers, esConfig.getServers());
        assertEquals(esRetries, esConfig.getRetries());
        assertEquals(esMaxRequestSize, esConfig.getMaxRequestSize());
        assertEquals(esStoreTimeoutMs, esConfig.getStoreTimeoutMs());
        assertEquals(esBatchSize, esConfig.getBatchSize());
        assertEquals(esMaxInFlightPerConnection, esConfig.getMaxInFlightPerConnection());
        assertEquals(esAck, esConfig.getAck());
        assertEquals(esCompressionType, esConfig.getCompressionType());
        assertEquals(esKeySerializer, esConfig.getKeySerializer());
        assertEquals(esValueSerializer, esConfig.getValueSerializer());

        assertEquals(null, defaultConfig.getTopic());
        assertEquals("localhost:9092", defaultConfig.getServers());
        assertEquals(1, defaultConfig.getRetries());
        assertEquals(1000000, defaultConfig.getMaxRequestSize());
        assertEquals(10000, defaultConfig.getStoreTimeoutMs());
        assertEquals(5000000, defaultConfig.getBatchSize());
        assertEquals(10, defaultConfig.getMaxInFlightPerConnection());
        assertEquals("1", defaultConfig.getAck());
        assertEquals("none", defaultConfig.getCompressionType());
        assertEquals("org.apache.kafka.common.serialization.StringSerializer", defaultConfig.getKeySerializer());
        assertEquals("org.apache.kafka.common.serialization.ByteArraySerializer", defaultConfig.getValueSerializer());
    }


    @Configuration
    static class TestContext {

        @Bean
        AttachmentKafkaConsumerConfig<String, byte[]> kafkaConsumerConfig() {
            return new AttachmentKafkaConsumerConfig<>();
        }

        @Bean
        @Scope(value = "prototype")
        KafkaProducerConfigBuilder<String, byte[]> kafkaProducerConfig() {
            return new KafkaProducerConfigBuilder<>();
        }

        @Bean(name = "defaultConfig")
        KafkaProducerConfigBuilder.KafkaProducerConfig defaultKafkaProducerConfig(KafkaProducerConfigBuilder<String, byte[]> kafkaProducerConfig) {
            return kafkaProducerConfig.getProducerConfig();
        }
            @Bean(name = "attachmentConfig")
        KafkaProducerConfigBuilder.KafkaProducerConfig attachmentKafkaProducerConfig(KafkaProducerConfigBuilder<String, byte[]> kafkaProducerConfig) {
            KafkaProducerConfigBuilder.KafkaProducerConfig conf = kafkaProducerConfig.getProducerConfig()
                    .withTopic(attTopic)
                    .withStoreTimeoutMs(attStoreTimeoutMs)
                    .withRetries(attRetries)
                    .withMaxRequestSize(attMaxRequestSize)
                    .withAck(attAck)
                    .withBatchSize(attBatchSize)
                    .withCompressionType(attCompressionType)
                    .withKeySerializer(attKeySerializer)
                    .withMaxInFlightPerConnection(attMaxInFlightPerConnection)
                    .withServers(attServers)
                    .withValueSerializer(attValueSerializer);
            return conf;
        }


        @Bean(name = "esConfig")
        KafkaProducerConfigBuilder.KafkaProducerConfig esKafkaProducerConfig(KafkaProducerConfigBuilder<String, byte[]> kafkaProducerConfig) {
            KafkaProducerConfigBuilder.KafkaProducerConfig conf = kafkaProducerConfig.getProducerConfig()
                    .withTopic(esTopic)
                    .withStoreTimeoutMs(esStoreTimeoutMs)
                    .withRetries(esRetries)
                    .withMaxRequestSize(esMaxRequestSize)
                    .withAck(esAck)
                    .withBatchSize(esBatchSize)
                    .withCompressionType(esCompressionType)
                    .withKeySerializer(esKeySerializer)
                    .withMaxInFlightPerConnection(esMaxInFlightPerConnection)
                    .withServers(esServers)
                    .withValueSerializer(esValueSerializer);
            return conf;
        }

    }
}
