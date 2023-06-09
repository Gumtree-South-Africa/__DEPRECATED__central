package com.ecg.comaas.kjca.filter.volumefilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
@Ignore
public class VolumeFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(new Properties(), null, 20);

    @Test
    public void violatesQuota() throws Exception {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"HOURS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 1, \"perTimeUnit\": \"DAYS\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "foo" + System.currentTimeMillis() + "@bar.com";
        for (int i = 0; i < 3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void skipsQuotaViolation() throws InterruptedException {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 3, \"perTimeUnit\": \"SECONDS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 10, \"perTimeUnit\": \"SECONDS\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "foo" + System.currentTimeMillis() + "@bar.com";
        for (int i = 0; i < 2; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(MessageState.SENT, response.getMessage().getState());
    }

    @Test
    public void remembersViolations() throws Exception {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse(
                "{rules: [" +
                        "{\"allowance\": 2, " +
                        "\"perTimeValue\":  8, " +
                        "\"perTimeUnit\": \"SECONDS\", " +
                        "\"score\": 100," +
                        "\"scoreMemoryDurationValue\": 16, " +
                        "\"scoreMemoryDurationUnit\": \"SECONDS\"}" +
                        "]}"));

        // Send 2 messages, hopefully within 8 seconds
        long start = System.currentTimeMillis();
        String from = "foo" + start + "@bar.com";
        MailBuilder mailBuilder = MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar");

        for (int i = 0; i < 2; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(mailBuilder);
            assertEquals("Expecting message state == SENT", MessageState.SENT, response.getMessage().getState());
        }

        // We've now violated the quota, so sending another message should fail
        MailInterceptor.ProcessedMail response = rule.deliver(mailBuilder);
        assertEquals("Expecting quota violation, failure to send", 1, response.getMessage().getProcessingFeedback().size());

        // Wait until the quota window has elapsed but the ttl on the in-memory violation store has not yet
        TimeUnit.SECONDS.sleep(10);

        // The violation has not expired yet, even though we're outside the quota window (due to the ttl), sending a message should fail
        response = rule.deliver(mailBuilder);
        assertEquals("Expecting TTL violation, failure to send", 1, response.getMessage().getProcessingFeedback().size());

        // Get outside the ttl
        TimeUnit.SECONDS.sleep(10);

        // And we should be able to send messages again
        response = rule.deliver(mailBuilder);
        assertEquals("Expecting message sent after TTL", MessageState.SENT, response.getMessage().getState());
    }

    @Test
    public void ignoresFollowUpEmails() throws Exception {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 1, \"perTimeValue\": 100, \"perTimeUnit\": \"SECONDS\", \"score\": 100}" +
                "    ], ignoreFollowUps: true\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        // email sent from platform (desktop/api) because X-ADID is set
        MailInterceptor.ProcessedMail response = rule.deliver(
                MailBuilder.aNewMail()
                        .adId("123")
                        .from(from)
                        .to("bar@foo.com")
                        .htmlBody("oobar"));
        assertEquals(MessageState.SENT, response.getMessage().getState());
        String buyerSecretAddress = "Buyer." + response.getConversation().getBuyerSecret() + "@test-platform.com";

        // seller responds many times . should be ignored by velocity filter
        for (int i = 0; i < 3; i++) {
            response = rule.deliver(
                    MailBuilder.aNewMail()
                            .to(buyerSecretAddress)
                            .from("bar@foo.com")
                            .htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            assertEquals(0, response.getMessage().getProcessingFeedback().size()); // shouldn't get scored
        }
    }
}
