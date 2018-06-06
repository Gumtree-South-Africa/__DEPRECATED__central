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

import static java.lang.String.format;

@Service
class KafkaMessageConsumerFactory {
    @Value("${kafka.core.servers}")
    private String bootstrapServers;

    @Value("${kafka.core.max.poll.records:1}")
    private Integer maxPollRecords;

    @Value("${kafka.core.max.poll.interval.ms:300000}")
    private Integer maxPollIntervalMs;

    Consumer<String, byte[]> createConsumer(final String topicName) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MailProcessingGroup_" + topicName);
        String allocId = System.getenv("NOMAD_ALLOC_ID");
        if (allocId != null) {
            long threadId = Thread.currentThread().getId();
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, format("%s-%d", allocId, threadId));
        }
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // Note that the value of max poll records should not change. When we read more messages than 1 off the topic,
        // the first one fails, and a later one succeeds, which offset do we commit? This is why we read 1 message
        // at a time.
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // Sometimes message processing takes a long time. This blocks other messages in the partition. Here we increase
        // the time Comaas is allowed to take before the broker takes away this partition. The default is 5m.
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        final KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topicName));
        return consumer;
    }
}
