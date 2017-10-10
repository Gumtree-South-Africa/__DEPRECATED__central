package com.ecg.replyts.core.runtime.persistence.kafka;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaSinkService {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSinkService.class);

    private Timer saveTimer;
    private Counter messageCounter;
    private KafkaProducerConfigBuilder.KafkaProducerConfig producerConfig;

    public KafkaSinkService(Timer saveTimer, Counter messageCounter, KafkaProducerConfigBuilder.KafkaProducerConfig producerConfig) {
        this.saveTimer = saveTimer;
        this.messageCounter = messageCounter;
        this.producerConfig = producerConfig;
    }

    public Future<RecordMetadata> storeAsync(String messagekey, byte[] content) {
        ProducerRecord<String, byte[]> rec = new ProducerRecord<>(producerConfig.getTopic(), messagekey, content);
        LOG.debug("Persisting key {} under topic {}", rec.key(), rec.topic());
        return producerConfig.getProducer().send(rec, (metadata, exception) -> {
            if (exception == null) {
                messageCounter.inc();
            } else {
                LOG.error("Error storing the value with messagekey={}", messagekey, exception);
            }
        });
    }

    public void store(String messagekey, byte[] content) {
        try (Timer.Context ignored = saveTimer.time()) {
            storeAsync(messagekey, content).get(producerConfig.getStoreTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
