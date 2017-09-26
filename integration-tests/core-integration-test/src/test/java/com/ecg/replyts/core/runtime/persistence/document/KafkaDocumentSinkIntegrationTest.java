package com.ecg.replyts.core.runtime.persistence.document;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaConsumerConfig;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
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
                try {
                    assertEquals(consumerConfig.getTopic(), record.topic());
                    assertEquals(msgkey, record.key());

                    JSONObject obj = new JSONObject(new String(record.value()));

                    assertEquals(toEmail, obj.getString("toEmail"));
                    assertEquals(fromEmail, obj.getString("fromEmail"));
                    assertEquals(msgText, obj.getString("messageText"));
                    assertEquals(adId, obj.getInt("adId"));
                    assertEquals(attachmentName, obj.getJSONArray("attachments").getString(0));
                } catch (JSONException e) {
                    Assert.fail(String.format("Expected valid JSON but got '%s' Error: %s", new String(record.value()), e.getMessage()));
                }
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
