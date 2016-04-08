package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class VolumeFilterIntegrationTest {


    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

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
            AwaitMailSentProcessedListener.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
        }
        // give Elastic search some time for flushing the index
        // this time is rather random - which makes the test very unstable.
        Thread.sleep(2000);

        AwaitMailSentProcessedListener.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }


    @Test
    @Ignore
     // test sleeps for a minute...
    public void skipsQuotaViolation() throws InterruptedException {

        rule.registerConfig(VolumeFilterFactory.class,  (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"MINUTES\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 10, \"perTimeUnit\": \"MINUTES\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "foo"+System.currentTimeMillis()+"@bar.com";
        for(int i = 0; i<2; i++) {
            AwaitMailSentProcessedListener.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
        }
        // give Elastic search some time for flushing the index
        // this time is rather random - which makes the test very unstable.
        TimeUnit.MINUTES.sleep(1);

        AwaitMailSentProcessedListener.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(MessageState.SENT, response.getMessage().getState());
    }
}
