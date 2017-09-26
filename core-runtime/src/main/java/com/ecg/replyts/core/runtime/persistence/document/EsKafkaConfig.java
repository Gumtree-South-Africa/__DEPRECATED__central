package com.ecg.replyts.core.runtime.persistence.document;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "ship.documents2kafka.enabled", havingValue = "true")
public class EsKafkaConfig {


    private static final Logger LOG = LoggerFactory.getLogger(EsKafkaConfig.class);

    @Value("${kafka.es.topic:esdocuments}")
    private String topic;

    @Bean
    KafkaProducerConfig<String, byte[]> defaultProducerConfig() {
        return new KafkaProducerConfig<>();
    }

    @Bean
    @Qualifier("esSink")
    public KafkaSinkService kafkaSinkService(KafkaProducerConfig<String, byte[]> defaultProducerConfig) {

        Timer save = TimingReports.newTimer("kafka.esSaveTimer");
        Counter attachment_counter = TimingReports.newCounter("kafka.esMessageCounter");

        KafkaProducerConfig producerConfig = defaultProducerConfig.withTopic(topic);
        producerConfig.init();
        LOG.debug("Initialized the Kafka Service for ES bean");
        return new KafkaSinkService(save, attachment_counter, producerConfig);
    }

}
