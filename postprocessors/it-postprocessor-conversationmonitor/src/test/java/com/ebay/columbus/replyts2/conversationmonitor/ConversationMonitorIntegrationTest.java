package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

/**
 * Created by fmaffioletti on 28/07/14.
 */
public class ConversationMonitorIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
            new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
                Properties properties = new Properties();
                properties.put("replyts.conversation.monitor.trigger.chars", "65533,189");
                properties.put("replyts.conversation.monitor.threshold.check.enabled", "true");
                properties.put("replyts.conversation.monitor.warn.size.threshold", "5");
                properties.put("replyts.conversation.monitor.error.size.threshold", "10");
                properties.put("replyts.conversation.monitor.replaced.chars", "$|a,&|b");
                return properties;
            }).get(), ConversationMoniitorPostProcessorConfiguration.class);

    @Test public void conversationMonitorFilterNoReplacedChars() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345"));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
        Assert.assertEquals("12345", processedMail.getMessage().getPlainTextBody());
    }

    @Test public void conversationMonitorFilterWithReplacedChars() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345 $ &"));
        List<String> plaintextParts = processedMail.getOutboundMail().getPlaintextParts();
        Assert.assertEquals("12345 a b", plaintextParts.get(0));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test public void conversationMonitorFilterWithTriggerChars() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345 � &"));
        List<String> plaintextParts = processedMail.getOutboundMail().getPlaintextParts();
        Assert.assertTrue(plaintextParts.get(0).startsWith("12345"));
        Assert.assertTrue(plaintextParts.get(0), plaintextParts.get(0).endsWith("b"));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }
}
