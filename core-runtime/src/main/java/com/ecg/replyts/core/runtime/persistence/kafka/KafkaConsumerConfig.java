package com.ecg.replyts.core.runtime.persistence.kafka;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

// This is used for testing only
@VisibleForTesting
@Component
@Scope("prototype")
public class KafkaConsumerConfig<K, V> {

    @Value("${kafka.core.servers:localhost:9092}")
    private String servers;

    @Value("${kafka.attachment.client_group_id:consumer}")
    private String group_id;

    @Value("${kafka.core.key.deserializer:org.apache.kafka.common.serialization.StringDeserializer}")
    private String key_deserializer;

    @Value("${kafka.core.value.deserializer:org.apache.kafka.common.serialization.ByteArrayDeserializer}")
    private String value_deserializer;

    private String topic;

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
        Preconditions.checkState(StringUtils.isNotBlank(topic), "Topic must be defined!");
        consumer = new KafkaConsumer<>(getConfigProperties());
        consumer.subscribe(Collections.singletonList(topic));
    }

    public Consumer<K, V> getConsumer() {
        return consumer;
    }

    public String getTopic() {
        return topic;
    }

    public KafkaConsumerConfig setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    @PreDestroy
    public void close() {
        consumer.close();
    }


}
