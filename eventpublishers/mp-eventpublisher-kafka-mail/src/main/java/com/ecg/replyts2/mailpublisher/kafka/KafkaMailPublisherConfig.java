package com.ecg.replyts2.mailpublisher.kafka;

import com.ecg.replyts.core.runtime.listener.KafkaMailPublisher;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

@Configuration
public class KafkaMailPublisherConfig {

    @Value("${mailpublisher.kafka.broker.list:localhost:9092}")
    private String kafkaBrokers;
    @Value("${mailpublisher.kafka.topic:coremail}")
    private String kafkaTopic;
    private Producer<String, byte[]> producer;

    @PostConstruct
    @Conditional(KafkaMailPublisherConditional.class)
    private void createKafkaProducer() {
        Assert.hasLength(kafkaBrokers);

        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaBrokers);
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<>(config);
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