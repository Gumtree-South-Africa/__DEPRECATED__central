package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.UserResponsivenessListener;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Component
@ConditionalOnBean(UserResponsivenessListener.class)
public class ResponsivenessKafkaProducer {

    @Value("${kafka.core.servers:kafkacore.service.consul:9092}")
    private String servers;

    @Value("${user-behaviour.responsiveness.queue.compressionType:none}")
    private String compressionType;

    @Value("${user-behaviour.responsiveness.queue.retries:1}")
    private int retries;

    @Value("${user-behaviour.responsiveness.queue.ack:1}")
    private String ack;

    @Value("${user-behaviour.responsiveness.queue.maxBlockMs:5000}")
    private String maxBlockMs;

    private KafkaProducer<String, ResponsivenessRecord> producer;

    @PostConstruct
    private void createKafkaProducer() {
        Properties configProperties = new Properties();
        configProperties.put(BOOTSTRAP_SERVERS_CONFIG, servers);
        configProperties.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProperties.put(VALUE_SERIALIZER_CLASS_CONFIG, ResponsivenessRecordSerializer.class);
        configProperties.put(COMPRESSION_TYPE_CONFIG, compressionType);
        configProperties.put(RETRIES_CONFIG, retries);
        configProperties.put(ACKS_CONFIG, ack);
        configProperties.put(MAX_BLOCK_MS_CONFIG, maxBlockMs);
        producer = new KafkaProducer<>(configProperties);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close(10, TimeUnit.SECONDS);
        }
    }

    public KafkaProducer<String, ResponsivenessRecord> getProducer() {
        return producer;
    }
}
