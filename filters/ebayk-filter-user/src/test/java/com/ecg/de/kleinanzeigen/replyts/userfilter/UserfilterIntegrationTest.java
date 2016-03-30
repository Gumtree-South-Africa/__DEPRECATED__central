package com.ecg.de.kleinanzeigen.replyts.userfilter;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class UserfilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule(10, "cassandra_schema.cql");

    @Before
    public void setUp() throws Exception {
          itRule.registerConfig(UserfilterFactory.class, (ObjectNode) JsonObjects.parse("{'rules': [{'regexp': 'buyer', 'score':2000}]}"));
    }

    @Test
    public void userFilterHits() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail processedMail = itRule.deliver(
                MailBuilder.aNewMail().adId("123").from("buyer@test.de").to("seller@test.de").htmlBody("hello world!"));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }
}
