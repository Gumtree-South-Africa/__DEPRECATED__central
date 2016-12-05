package com.ecg.de.kleinanzeigen.hadoop;

/**
 * Created by johndavis on 30/11/16.
 */

import com.ecg.de.kleinanzeigen.AsyncProducer;
import com.ecg.de.kleinanzeigen.UUIDSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.UUID;

/**
 * Configuration for Kafka.
 * <p>
 * Note: For performance reasons it is recommended to use the same producer instance to enable batching of events.
 */
@Configuration
public class KafkaHadoopLogConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaHadoopLogConfig.class);

    /**
     * All producers have to send their data to kbrokeraggr.
     * We want to write less data to local kafka and avoid mirror maker traffic.
     * Flume consumes from kbrokeraggr and so we send the data directly to aggr because
     */
    @Value("${dailyreport.kafka.aggregate.hosts:replyts.dev.kjdev.ca:9092}")
    private String kafkaHosts;


    private AsyncProducer<UUID, String> producer;


    @PostConstruct
    public void setup() {

        try {
            LOG.info("KAFKA: try connecting to {}", kafkaHosts);

            Properties props = producerProperties();
            producer = new AsyncProducer<>(new KafkaProducer<UUID, String>(props, new UUIDSerializer(), new StringSerializer()));
        } catch (Exception e) {
            LOG.error("Exception on Kafka config (hosts: " + kafkaHosts + "), it won't be possible to send events to Kafka", e);
        }
    }


    /**
     * Central producer with key=UUID and value=String.
     * Producer is thread save, should be shared for performance reasons if possible.
     */
    @Bean(name = "kafkaUUIDProducer")
    public AsyncProducer<UUID, String> getKafkaLongAvroProducer() {
        return producer;
    }

    public String getKafkaHosts() {
        return kafkaHosts;
    }


    private Properties producerProperties() {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts);
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        // batch size is mount of bytes to allocate for new ByteBuffer on batching.
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16 * 1024);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 500);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 32 * 1024 * 1024);

        return props;
    }

    @PreDestroy
    public void stop() {
        producer.close();
    }

}
