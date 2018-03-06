package com.ecg.messagebox.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

@Configuration
@ConditionalOnProperty(value = "replyts.message-added-events.publisher.enabled", havingValue = "true")
public class MessageAddedPublisherConfig {
    @Value("${replyts.kafka.broker.list:#{null}}")
    private String kafkaBrokers;
    @Value("${replyts.message-added-events.kafka.topic:message-added-events}")
    private String topic;

    private Producer<String, byte[]> producer;

    @PostConstruct
    private void createKafkaProducer() {
        checkNotNull(kafkaBrokers);

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaBrokers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("acks", "1");

        producer = new KafkaProducer<>(props);
    }

    @Bean
    public MessageAddedKafkaPublisher messageAddedEventPublisher() {
        return new MessageAddedKafkaPublisher(producer, topic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
