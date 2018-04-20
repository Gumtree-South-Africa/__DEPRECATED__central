package com.ecg.replyts.app.mailreceiver.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Properties;

@Service
class KafkaMessageConsumerFactory {
    @Value("${kafka.core.servers}")
    private String bootstrapServers;

    Consumer<String, byte[]> createConsumer(final String topicName) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MailProcessingGroup_" + topicName);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        // Note that the value of max poll records should not change. When we read more messages than 1 off the topic,
        // the first one fails, and a later one succeeds, which offset do we commit? This is why we read 1 message
        // at a time.
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        final KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topicName));
        return consumer;
    }
}
