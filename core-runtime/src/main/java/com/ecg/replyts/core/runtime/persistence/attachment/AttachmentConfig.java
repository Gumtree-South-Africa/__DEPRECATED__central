package com.ecg.replyts.core.runtime.persistence.attachment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
public class AttachmentConfig {

    @Bean
    public SwiftAttachmentRepository swiftAttachmentRepository() {
        return new SwiftAttachmentRepository();
    }

    @Bean
    public AttachmentKafkaProducerConfig<String, byte[]> kafkaProducerConfig() {
        return new AttachmentKafkaProducerConfig<>();
    }

    @Bean
    public AttachmentKafkaSinkService kafkaSinkService() {
        return new AttachmentKafkaSinkService();
    }

    @Bean
    public AttachmentRepository attachmentRepository() {
        return new AttachmentRepository();
    }
}
