package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class IpRiskFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule(10);

    @Before
    public void setUp() throws Exception {
          itRule.registerConfig(
                  IpRiskFilterFactory.class,
                  JsonObjects.builder().attr("GOOD","20").attr("BAD", "20").attr("MEDIUM_BAD", "20").attr("VERY_BAD", "20").build());
    }

    // Test deactivated, use only for manual testing.
    @Ignore
    @Test
    public void userFilterHits() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().header("X-CUST-IP","194.50.69.177").adId("123").from("buyer@test.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
        assertEquals(20, processedMail.getMessage().getProcessingFeedback().get(0).getScore().intValue());
    }
}
