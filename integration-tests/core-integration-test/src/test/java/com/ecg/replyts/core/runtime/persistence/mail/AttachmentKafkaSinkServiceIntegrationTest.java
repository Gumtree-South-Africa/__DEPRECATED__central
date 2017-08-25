package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentKafkaConsumerConfig;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentKafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentKafkaSinkService;
import com.google.common.net.MediaType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Ignore
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AttachmentKafkaSinkServiceIntegrationTest.TestContext.class)
public class AttachmentKafkaSinkServiceIntegrationTest {

    @Autowired
    private AttachmentKafkaSinkService attachmentSinkService;

    @Autowired
    private AttachmentKafkaConsumerConfig<String, byte[]> consumerConfig;


    String container = "container";
    String messageid = UUID.randomUUID().toString();
    String attachname = "filesdalfk lsakdf asdkfk lsadfklfkl k;lkfgoop34534534l5jkgl    er jgelr gklerjlkgjlejr gj as1.txt";
    String msgkey = container + "/" + messageid + "/" + attachname;

    static class AttachmentTypedContent extends TypedContent {

        public AttachmentTypedContent(MediaType type, byte[] data) {
            super(type, data);
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public void overrideContent(Object newContent) throws IllegalStateException {
            throw new UnsupportedOperationException();
        }
    }

    TypedContent<byte[]> content = new AttachmentTypedContent(MediaType.PLAIN_TEXT_UTF_8, new byte[]{55, 56, 57, 58, 23, 34, 45, 67, 46, 4, 76, 60});

    @Test
    public void storeAttachment() {
        // Store one record
        attachmentSinkService.store(msgkey, content);
        // Read the record
        readAttachment();
    }

    public void readAttachment() {
        try (Consumer<String, byte[]> consumer = consumerConfig.getConsumer()) {
            // It will not actually wait for 5s, its a max wait only
            ConsumerRecords<String, byte[]> records = consumer.poll(5000);

            assertEquals("Expecting one record only", 1, records.count());

            for (ConsumerRecord<String, byte[]> record : records) {
                assertEquals(attachmentSinkService.getTopic(), record.topic());
                assertEquals(msgkey, record.key());
                assertArrayEquals(content.getContent(), record.value());
            }
        }
    }

    @Ignore
    @Configuration
    static class TestContext {

        @Bean
        AttachmentKafkaConsumerConfig<String, byte[]> kafkaConsumerConfig() {
            return new AttachmentKafkaConsumerConfig<>();
        }

        @Bean
        AttachmentKafkaSinkService kafkaSinkService() {
            return new AttachmentKafkaSinkService();
        }

        @Bean
        AttachmentKafkaProducerConfig<String, byte[]> kafkaProducerConfig() {
            return new AttachmentKafkaProducerConfig<>();
        }

    }
}
