package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class VolumeFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);

    @Test
    public void violatesQuota() throws Exception {
        rule.registerConfig(VolumeFilterFactory.class,  (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"HOURS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 1, \"perTimeUnit\": \"DAYS\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "foo"+System.currentTimeMillis()+"@bar.com";
        for(int i = 0; i<3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            rule.waitUntilIndexedInEs(response);
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void doesNotViolateQuotaIfWhitelistedEmail() throws Exception {
        rule.registerConfig(VolumeFilterFactory.class,  (ObjectNode) JsonObjects.parse("{\n" +
                "    whitelistedEmails: [\"no-reply@gumtree.com.au\"],\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"HOURS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 1, \"perTimeUnit\": \"DAYS\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "no-reply@gumtree.com.au";
        for(int i = 0; i<3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar").customHeader("Contact-Type", "CALL_BACK_REQUEST"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            // give Elastic search some time for flushing the index
            // this time is rather random - which makes the test very unstable.
            rule.waitUntilIndexedInEs(response);
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar").customHeader("Contact-Type", "CALL_BACK_REQUEST"));
        assertEquals(0, response.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void skipsQuotaViolation() throws InterruptedException {
        rule.registerConfig(VolumeFilterFactory.class,  (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"MINUTES\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 10, \"perTimeUnit\": \"MINUTES\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));

        String from = "foo"+System.currentTimeMillis()+"@bar.com";
        for(int i = 0; i<2; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            rule.waitUntilIndexedInEs(response);
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(MessageState.SENT, response.getMessage().getState());
    }
}
