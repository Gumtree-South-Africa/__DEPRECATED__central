package com.ecg.replyts.core.runtime.persistence.attachment;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
public class AttachmentConfig {

    @Value("${kafka.attachment.compressionType:none}")
    private String compressionType;

    @Value("${kafka.attachment.batch.size:16000000}")
    private int batchSize;

    @Value("${kafka.attachment.max.request.size:15500000}")
    private int maxRequestSize;

    @Value("${kafka.attachment.topic:attachment}")
    private String topic;

    @Value("${kafka.attachment.request.timeout.ms:10000}")
    private int storeTimeoutMs;

    @Bean
    KafkaProducerConfig<String, byte[]> defaultProducerConfig() {
        return new KafkaProducerConfig<>();
    }

    @Bean(name = "attachmentSink")
    public KafkaSinkService kafkaSinkService(KafkaProducerConfig<String, byte[]> defaultProducerConfig) {

        KafkaProducerConfig<String, byte[]> attachmentProducerConfig = defaultProducerConfig
                .withTopic(topic)
                .withStoreTimeoutMs(storeTimeoutMs)
                .withBatchSize(batchSize)
                .withCompressionType(compressionType)
                .withMaxRequestSize(maxRequestSize);
        attachmentProducerConfig.init();

        Timer save = TimingReports.newTimer("attachment.kafka-save-timer");
        Counter attachment_counter = TimingReports.newCounter("attachment.kafka-attachment-counter");
        return new KafkaSinkService(save, attachment_counter, attachmentProducerConfig);
    }

    @Bean
    public SwiftAttachmentRepository swiftAttachmentRepository() {
        return new SwiftAttachmentRepository();
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
        return new AttachmentRepository();
    }
}
