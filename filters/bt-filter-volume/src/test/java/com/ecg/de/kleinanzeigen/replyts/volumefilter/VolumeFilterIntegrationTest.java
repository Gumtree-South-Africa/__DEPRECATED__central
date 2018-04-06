package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.junit.Assert.assertEquals;

public class VolumeFilterIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);

    @Test
    public void violatesQuota() throws Exception {
        rule.registerConfig(VolumeFilterFactory.class, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"HOURS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 1, \"perTimeUnit\": \"DAYS\", \"score\": 200}\n" +
                "    ], runFor : { \"exceptCategories\": [], \"categories\":[]},ignoreFollowUps: true\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        for (int i = 0; i < 3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        rule.flushSearchIndex();

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void skipsQuotaViolation() throws InterruptedException {
        rule.registerConfig(VolumeFilterFactory.class, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 3, \"perTimeUnit\": \"SECONDS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 10, \"perTimeUnit\": \"SECONDS\", \"score\": 200}\n" +
                "    ],runFor : { \"exceptCategories\": [], \"categories\":[]},ignoreFollowUps: true\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        for (int i = 0; i < 2; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        TimeUnit.SECONDS.sleep(3);

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(MessageState.SENT, response.getMessage().getState());
    }

    @Test
    public void remembersViolations() throws Exception {
        rule.registerConfig(VolumeFilterFactory.class, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 4, \"perTimeUnit\": \"SECONDS\", \"score\": 100," +
                "           \"scoreMemoryDurationValue\": 10, \"scoreMemoryDurationUnit\": \"SECONDS\"}\n" +
                "    ],runFor : { \"exceptCategories\": [], \"categories\":[]},ignoreFollowUps: true\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        for (int i = 0; i < 3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        rule.flushSearchIndex();

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(1, response.getMessage().getProcessingFeedback().size());

        // violation memory window starts

        TimeUnit.SECONDS.sleep(4); // should be long enough to get outside the quota window, but before violation expires

        response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(1, response.getMessage().getProcessingFeedback().size()); // still remember violation

        TimeUnit.SECONDS.sleep(4); // should be long enough to "forget" the violation

        // violation memory window ends

        response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(MessageState.SENT, response.getMessage().getState());
    }

    @Test
    public void ignoresFollowUpEmails() throws Exception {
        rule.registerConfig(VolumeFilterFactory.class, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 1, \"perTimeValue\": 100, \"perTimeUnit\": \"SECONDS\", \"score\": 100}" +
                "    ], runFor : { \"exceptCategories\": [], \"categories\":[]},ignoreFollowUps: true\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        // email sent from platform (desktop/api) because X-ADID is set
        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));

        assertEquals(MessageState.SENT, response.getMessage().getState());
        String buyerSecretAddress = "Buyer." + response.getConversation().getBuyerSecret() + "@test-platform.com";

        // seller responds many times . should be ignored by velocity filter
        for (int i = 0; i < 3; i++) {
            response = rule.deliver(MailBuilder.aNewMail().to(buyerSecretAddress).from("bar@foo.com").customHeader("conversation_id", response.getConversation().getId()).htmlBody("oobar"));

            assertEquals(MessageState.SENT, response.getMessage().getState());
            assertEquals(0, response.getMessage().getProcessingFeedback().size()); // shouldn't get scored
        }
    }
}