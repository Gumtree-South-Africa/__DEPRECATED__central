package com.ecg.replyts.core.runtime.persistence.attachment;

import com.ecg.replyts.core.runtime.persistence.queue.KafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.queue.KafkaSinkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
public class AttachmentConfiguration {

    @Bean
    public SwiftAttachmentRepository swiftAttachmentRepository() {
        return new SwiftAttachmentRepository();
    }

    @Bean
    public KafkaProducerConfig<String, byte[]> kafkaProducerConfig() {
        return new KafkaProducerConfig<>();
    }

    @Bean
    public KafkaSinkService kafkaSinkService() {
        return new KafkaSinkService();
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
        return new AttachmentRepository();
    }
}
