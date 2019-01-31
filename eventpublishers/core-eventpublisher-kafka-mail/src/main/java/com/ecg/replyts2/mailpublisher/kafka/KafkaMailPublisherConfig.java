package com.ecg.replyts2.mailpublisher.kafka;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.listener.KafkaMailPublisher;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@ComaasPlugin
@Configuration
@ConditionalOnProperty(name = "mailpublisher.kafka.enabled", havingValue = "true")
public class KafkaMailPublisherConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMailPublisherConfig.class);

    @Value("${mailpublisher.kafka.broker.list:#{null}}")
    private String kafkaBrokers;
    @Value("${mailpublisher.kafka.topic:coremail}")
    private String kafkaTopic;
    private KafkaProducer<String, byte[]> producer;

    @PostConstruct
    private void createKafkaProducer() {
        checkNotNull(kafkaBrokers);
        LOGGER.info("Initializing KafkaMailPublisherConfig with kafkaBrokers {} and kafkaTopic {}", kafkaBrokers, kafkaTopic);
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        props.put(ACKS_CONFIG, "1");

        producer = new KafkaProducer<>(props);
    }

    @Bean
    public MailPublisher kafkaMailPublisher() {
        return new KafkaMailPublisher(producer, kafkaTopic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
        producer = null;
    }
}
