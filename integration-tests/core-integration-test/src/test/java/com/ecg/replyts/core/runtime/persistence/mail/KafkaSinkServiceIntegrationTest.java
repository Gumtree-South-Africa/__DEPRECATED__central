package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.persistence.queue.KafkaConsumerConfig;
import com.ecg.replyts.core.runtime.persistence.queue.KafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.queue.KafkaSinkService;
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
@ContextConfiguration(classes = KafkaSinkServiceIntegrationTest.TestContext.class)
public class KafkaSinkServiceIntegrationTest {

    @Autowired
    private KafkaSinkService attachmentSinkService;

    @Autowired
    private KafkaConsumerConfig<String, byte[]> consumerConfig;


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
        KafkaConsumerConfig<String, byte[]> kafkaConsumerConfig() {
            return new KafkaConsumerConfig<>();
        }

        @Bean
        KafkaSinkService kafkaSinkService() {
            return new KafkaSinkService();
        }

        @Bean
        KafkaProducerConfig<String, byte[]> kafkaProducerConfig() {
            return new KafkaProducerConfig<>();
        }

    }
}
