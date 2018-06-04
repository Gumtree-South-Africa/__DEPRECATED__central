package com.ecg.replyts.core.runtime.persistence.document;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaProducerConfigBuilder;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsKafkaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EsKafkaConfig.class);

    @Value("${kafka.es.topic:esdocuments}")
    private String topic;

    // Send this many messages to broker without waiting for response, increase throughput
    @Value("${kafka.es.max-in-flight-request-per-connection:50}")
    private int maxInFlightRequests;

    // Sending in batch more efficiently
    @Value("${kafka.es.batch.size:5000000}")
    private int batchSize;

    @Bean
    KafkaProducerConfigBuilder<String, byte[]> defaultKafkaProducerConfig() {
        return new KafkaProducerConfigBuilder<>();
    }

    @Bean(name = "esSink")
    public KafkaSinkService kafkaSinkService( KafkaProducerConfigBuilder<String, byte[]> defaultKafkaProducerConfig) {

        Timer save = TimingReports.newTimer("kafka.esSaveTimer");
        Counter attachment_counter = TimingReports.newCounter("kafka.esMessageCounter");

        KafkaProducerConfigBuilder.KafkaProducerConfig producerConfig = defaultKafkaProducerConfig
                .getProducerConfig()
                .withMaxInFlightPerConnection(maxInFlightRequests)
                .withBatchSize(batchSize)
                .withLingerMs(1000)
                .withTopic(topic);

        LOG.debug("Initialized the Kafka Service for ES bean");
        return new KafkaSinkService(save, attachment_counter, producerConfig.build());
    }

}
