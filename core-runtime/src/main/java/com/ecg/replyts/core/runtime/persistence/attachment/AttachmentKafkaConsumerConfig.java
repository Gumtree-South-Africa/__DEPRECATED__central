package com.ecg.replyts.core.runtime.persistence.attachment;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

public class AttachmentKafkaConsumerConfig<K, V> {

    @Value("${kafka.attachment.servers:replyts.dev.kjdev.ca:9092}")
    private String servers;

    @Value("${kafka.attachment.topic:attachment}")
    private String topic;

    @Value("${kafka.attachment.client_group_id:tenant-attachment-consumer}")
    private String group_id;

    @Value("${kafka.attachment.key.deserializer:org.apache.kafka.common.serialization.StringDeserializer}")
    private String key_deserializer;

    @Value("${kafka.attachment.client_group_id:org.apache.kafka.common.serialization.ByteArrayDeserializer}")
    private String value_deserializer;

    private Consumer<K, V> consumer;

    private Properties getConfigProperties() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(GROUP_ID_CONFIG, group_id);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, key_deserializer);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, value_deserializer);
        return props;
    }

    @PostConstruct
    private void createKafkaConsumer() {
        consumer = new KafkaConsumer<>(getConfigProperties());
        consumer.subscribe(Collections.singletonList(topic));
    }

    public Consumer<K, V> getConsumer() {
        return consumer;
    }

    @PreDestroy
    public void close() {
        consumer.close();
    }
}
