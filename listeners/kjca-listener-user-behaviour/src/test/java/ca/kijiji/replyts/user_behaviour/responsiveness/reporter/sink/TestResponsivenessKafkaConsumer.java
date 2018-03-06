package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Collections;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Component
public class TestResponsivenessKafkaConsumer {

    private static final String CONSUMER_GROUP_ID = "user-responsiveness-consumer";
    private static final String OFFSET_STRATEGY = "earliest";

    @Value("${kafka.core.servers:#{null}}")
    private String servers;

    @Value("${user-behaviour.responsiveness.queue.topic:userresponsiveness_ca}")
    private String topic;

    private KafkaConsumer<String, ResponsivenessRecord> consumer;

    @PostConstruct
    private void createKafkaConsumer() {
        Properties configProperties = new Properties();
        configProperties.put(BOOTSTRAP_SERVERS_CONFIG, servers);
        configProperties.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProperties.put(VALUE_DESERIALIZER_CLASS_CONFIG, TestResponsivenessRecordDeserializer.class);
        configProperties.put(GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        configProperties.put(AUTO_OFFSET_RESET_CONFIG, OFFSET_STRATEGY);
        consumer = new KafkaConsumer<>(configProperties);
        consumer.subscribe(Collections.singletonList(topic));
    }

    @PreDestroy
    public void close() {
        if (consumer != null) {
            consumer.close();
        }
    }

    public KafkaConsumer<String, ResponsivenessRecord> getConsumer() {
        return consumer;
    }
}
