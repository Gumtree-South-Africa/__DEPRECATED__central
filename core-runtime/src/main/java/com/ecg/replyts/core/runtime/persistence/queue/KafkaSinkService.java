package com.ecg.replyts.core.runtime.persistence.queue;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaSinkService {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaSinkService.class);

    private static final Timer SAVE = TimingReports.newTimer("attachment.kafka-save-timer");
    private static final Counter ATTACHMENT_COUNTER = TimingReports.newCounter("attachment.kafka-attachment-counter");

    @Value("${kafka.request.timeout.ms:10000}")
    private int storeTimeoutMs;

    @Value("${kafka.attachment.topic:attachment}")
    private String topic;

    @Autowired
    private KafkaProducerConfig<String, byte[]> producerConfig;

    public String getTopic() {
        return topic;
    }

    private Future<RecordMetadata> storeAsync(String messagekey, TypedContent<byte[]> contents) {
        ProducerRecord<String, byte[]> rec = new ProducerRecord<>(topic, messagekey, contents.getContent());
        LOG.debug("Persisting key {} under topic {}", rec.key(), rec.topic());
        return producerConfig.getProducer().send(rec, (metadata, exception) -> {
            if (exception == null) {
                ATTACHMENT_COUNTER.inc();
            } else {
                LOG.error("Error storing the attachment with messagekey={}", messagekey, exception);
            }
        });
    }

    public void store(String messagekey, TypedContent<byte[]> contents) {
        try (Timer.Context ignored = SAVE.time()) {
            storeAsync(messagekey, contents).get(storeTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
