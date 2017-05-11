package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailInterceptor;
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
public class Ip2CountryFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule();

    @Before
    public void setUp() throws Exception {
          itRule.registerConfig(
                  Ip2CountryFilterFactory.class,
                  JsonObjects.builder().attr("DEFAULT","50").attr("DE", "0").attr("NL", "200").build());
    }

    // Test deactivated, use only for manual testing.
    @Ignore
    @Test
    public void ip2CountryFilterHits() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().header("X-CUST-IP","91.211.73.240").adId("123").from("buyer@test.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
        assertEquals(200, processedMail.getMessage().getProcessingFeedback().get(0).getScore().intValue());
    }
}
