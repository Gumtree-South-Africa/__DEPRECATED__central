package com.ecg.messagebox.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(value = "replyts.convupdate.publisher.enabled", havingValue = "true")
public class ConversationUpdatePublisherConfig {

    @Value("${replyts.kafka.broker.list:localhost:9092}")
    private String kafkaBrokers;
    @Value("${replyts.convupdate.kafka.topic:conversation_updates}")
    private String topic;

    private Producer<String, byte[]> producer;

    @PostConstruct
    private void createKafkaProducer() {
        Assert.hasLength(kafkaBrokers);

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaBrokers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("acks", "1");

        producer = new KafkaProducer<>(props);
    }

    @Bean
    public ConversationUpdateKafkaPublisher convUpdateKafkaPublisher() {
        return new ConversationUpdateKafkaPublisher(producer, topic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}