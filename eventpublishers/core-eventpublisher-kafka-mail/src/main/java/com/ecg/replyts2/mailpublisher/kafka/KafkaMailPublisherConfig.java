package com.ecg.replyts2.mailpublisher.kafka;

import com.ecg.replyts.core.runtime.listener.KafkaMailPublisher;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
public class KafkaMailPublisherConfig {
    @Value("${kafka.core.servers:kafka.service.consul:9092}")
    private String kafkaBrokers;
    @Value("${mailpublisher.kafka.topic:coremail}")
    private String kafkaTopic;
    private KafkaProducer<String, byte[]> producer;

    @PostConstruct
    @Conditional(KafkaMailPublisherConditional.class)
    private void createKafkaProducer() {
        checkNotNull(kafkaBrokers);

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        props.put(ACKS_CONFIG, "1");

        producer = new KafkaProducer<>(props);
    }

    @Bean
    @Conditional(KafkaMailPublisherConditional.class)
    public MailPublisher kafkaMailPublisher() {
        return new KafkaMailPublisher(producer, kafkaTopic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
        producer = null;
    }
}
