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

    // Do not change this unless you also change request.timeout.ms
    /**
     *  There is a new configuration max.poll.interval.ms which controls the maximum time between poll invocations before the
     *  consumer will proactively leave the group (5 minutes by default). The value of the configuration request.timeout.ms must always be larger
     *  than max.poll.interval.ms because this is the maximum time that a JoinGroup request can block on the server while
     *  the consumer is rebalancing, so we have changed its default value to just above 5 minutes.
     */
    private Integer maxPollIntervalMs = 300000;

    Consumer<String, byte[]> createConsumer(final String topicName) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "MailProcessingGroup_" + topicName);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // Note that the value of max poll records should not change. When we read more messages than 1 off the topic,
        // the first one fails, and a later one succeeds, which offset do we commit? This is why we read 1 message
        // at a time.
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // Sometimes message processing takes a long time. This blocks other messages in the partition. Here we increase
        // the time Comaas is allowed to take before the broker takes away this partition. The default is 5m.
        // Reconsider after upgrading to Kafka 2.0
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, maxPollIntervalMs+1);

        // Taking into account that we commit offset manually (see com.ecg.replyts.app.mailreceiver.kafka.KafkaMessageProcessor.doCommitSync)
        // autocommit in background can lead to unexpected behavior on consuming side
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        final KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topicName));
        return consumer;
    }

    public int getMaxPollIntervalMs() {
        return maxPollIntervalMs;
    }
}
