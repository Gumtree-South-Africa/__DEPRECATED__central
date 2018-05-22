package com.ecg.replyts.core.runtime.persistence.document;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaConsumerConfig;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@Ignore
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = KafkaDocumentSinkIntegrationTest.TestContext.class)
public class KafkaDocumentSinkIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

    @Autowired
    private KafkaConsumerConfig<String, byte[]> consumerConfig;

    private String fromEmail = "foo@bar.com";
    private String toEmail = "bar@foo.com";
    private int adId = 987634;
    private String msgText = "Hello world!";
    private String attachmentName = "attName.txt";
    private String tenant = "unknown";

    @Test
    public void storeAttachment() throws InterruptedException {

        MailInterceptor.ProcessedMail processedMail = testRule.deliver(MailBuilder.aNewMail()
                .from(fromEmail).to(toEmail).adId(String.format("%d", adId)).htmlBody(msgText).attachment(attachmentName, "value".getBytes()));

        Message msg = processedMail.getMessage();
        Conversation conversation = processedMail.getConversation();

        String msgkey = tenant + "/" + conversation.getId() + "/" + msg.getId();

        testRule.waitForMail();
        // Read the record
        readDocument(msgkey);

    }

    public void readDocument(String msgkey) {
        try (Consumer<String, byte[]> consumer = consumerConfig.getConsumer()) {
            // It will not actually wait for 5s, its a max wait only
            ConsumerRecords<String, byte[]> records = consumer.poll(500);

            assertEquals("Expecting one record only", 1, records.count());

            for (ConsumerRecord<String, byte[]> record : records) {
                assertEquals(consumerConfig.getTopic(), record.topic());
                assertEquals(msgkey, record.key());

                JsonParser jsonParser = new JsonParser();
                JsonObject obj = (JsonObject) jsonParser.parse(new String(record.value()));

                assertEquals(toEmail, obj.get("toEmail").getAsString());
                assertEquals(fromEmail, obj.get("fromEmail").getAsString());
                assertEquals(msgText, obj.get("messageText").getAsString());
                assertEquals(adId, obj.get("adId").getAsInt());
                assertEquals(attachmentName, obj.getAsJsonArray("attachments").get(0).getAsString());
            }
        }
    }

    @Configuration
    static class TestContext {

        @Bean
        KafkaConsumerConfig<String, byte[]> kafkaConsumerConfig() {
            KafkaConsumerConfig<String, byte[]> consumerConfig = new KafkaConsumerConfig<>();
            consumerConfig.setTopic("esdocuments");
            return consumerConfig;
        }

    }


}
