package com.ecg.de.kleinanzeigen.replyts.userfilter;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class UserfilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule();

    @Before
    public void setUp() throws Exception {
          itRule.registerConfig(UserfilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{'rules': [{'regexp': 'buyer', 'score':2000}]}"));
    }

    @Test
    public void userFilterHits() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().adId("123").from("buyer@test.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }
}
