package com.ecg.comaas.it.postprocessor.conversationmonitor;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

/**
 * Created by fmaffioletti on 28/07/14.
 */
public class ConversationMonitorIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
            new ReplyTsIntegrationTestRule(createProperties(), ConversationMonitorPostProcessor.class);

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_IT);
        properties.put("replyts.conversation.monitor.trigger.chars", "65533,189");
        properties.put("replyts.conversation.monitor.threshold.check.enabled", "true");
        properties.put("replyts.conversation.monitor.warn.size.threshold", "5");
        properties.put("replyts.conversation.monitor.error.size.threshold", "10");
        properties.put("replyts.conversation.monitor.replaced.chars", "$|a,&|b");
        return properties;
    }

    @Test public void conversationMonitorFilterNoReplacedChars() {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345"));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
        Assert.assertEquals("12345", processedMail.getMessage().getPlainTextBody());
    }

    @Test public void conversationMonitorFilterWithReplacedChars() {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345 $ &"));
        List<String> plaintextParts = processedMail.getOutboundMail().getPlaintextParts();
        Assert.assertEquals("12345 a b", plaintextParts.get(0));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test public void conversationMonitorFilterWithTriggerChars() {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("12345 � &"));
        List<String> plaintextParts = processedMail.getOutboundMail().getPlaintextParts();
        Assert.assertTrue(plaintextParts.get(0).startsWith("12345"));
        Assert.assertTrue(plaintextParts.get(0), plaintextParts.get(0).endsWith("b"));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }
}
