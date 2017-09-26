package com.ecg.replyts.core.runtime.persistence.mail;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentKafkaConsumerConfig;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaProducerConfig;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;

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
    private KafkaSinkService kafkaSinkService;

    @Autowired
    private AttachmentKafkaConsumerConfig<String, byte[]> consumerConfig;

    @Autowired
    KafkaProducerConfig<String, byte[]> kafkaProducerConfig;

    String container = "container";
    String messageid = UUID.randomUUID().toString();
    String attachname = "filesdalfk lsakdf asdkfk lsadfklfkl k;lkfgoop34534534l5jkgl    er jgelr gklerjlkgjlejr gj as1.txt";
    String msgkey = container + "/" + messageid + "/" + attachname;
    final byte[] content = new byte[]{55, 56, 57, 58, 23, 34, 45, 67, 46, 4, 76, 60};

    @Test
    public void storeAttachment() {
        // Store one record
        kafkaSinkService.store(msgkey, content);
        // Read the record
        readAttachment();
    }

    public void readAttachment() {
        try (Consumer<String, byte[]> consumer = consumerConfig.getConsumer()) {
            // It will not actually wait for 5s, its a max wait only
            ConsumerRecords<String, byte[]> records = consumer.poll(3000);

            assertEquals("Expecting one record only", 1, records.count());

            for (ConsumerRecord<String, byte[]> record : records) {
                assertEquals(consumerConfig.getTopic(), record.topic());
                assertEquals(msgkey, record.key());
                assertArrayEquals(content, record.value());
            }

        }
    }

    @Configuration
    static class TestContext {

        @Bean
        AttachmentKafkaConsumerConfig<String, byte[]> kafkaConsumerConfig() {
            return new AttachmentKafkaConsumerConfig<>();
        }

        @Bean
        KafkaProducerConfig<String, byte[]> defaultKafkaProducerConfig() {
            return new KafkaProducerConfig<>();
        }

        @Bean
        KafkaProducerConfig<String, byte[]> kafkaProducerConfig(KafkaProducerConfig<String, byte[]> defaultKafkaProducerConfig) {
            KafkaProducerConfig<String, byte[]> attachmentConfig = defaultKafkaProducerConfig.withTopic("attachments");
            attachmentConfig.init();
            return attachmentConfig;
        }

        @Bean
        KafkaSinkService kafkaSinkService(KafkaProducerConfig<String, byte[]> kafkaProducerConfig) {

            Timer save = TimingReports.newTimer("attachment.test-kafka-save-timer");
            Counter attachment_counter = TimingReports.newCounter("attachment.test-kafka-attachment-counter");
            return new KafkaSinkService(save, attachment_counter, kafkaProducerConfig);
        }

    }
}
