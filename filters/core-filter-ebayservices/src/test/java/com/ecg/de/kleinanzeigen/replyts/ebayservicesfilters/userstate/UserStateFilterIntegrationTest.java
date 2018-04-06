package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
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
public class UserStateFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule();

    @Before
    public void setUp() throws Exception {
          itRule.registerConfig(
                  UserStateFilterFactory.IDENTIFIER,
                  JsonObjects.builder().attr("UNKNOWN", "1").attr("CONFIRMED", "1").attr("SUSPENDED", "1").build());
    }

    // Test deactivated, use only for manual testing.
    @Ignore
    @Test
    public void userFilterDoesNotHit() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().adId("123").from("tester@ebay.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }

    // Test deactivated, use only for manual testing.
    @Ignore
    @Test
    public void userFilterDoesHit() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().adId("123").from("matthias.huttar@gmx.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }
}
