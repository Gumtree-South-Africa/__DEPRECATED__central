package com.ebay.columbus.replyts2.blockeduser;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by ddallemule on 2/10/14.
 */
public class BlockedUserFilterIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
                    new ReplyTsIntegrationTestRule();

    @Before public void setUp() throws Exception {
        replyTsIntegrationTestRule.registerConfig(BlockedUserFilterFactory.class, null);
    }

    @Test public void testBlockedUser() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().from("gonzalo@loquo.com").to("bar@foo.com")
                                        .htmlBody("this is a message! ")
                                        .customHeader("buyer-name", "BuyerName"));

        assertEquals(MessageState.BLOCKED, processedMail.getMessage().getState());
    }

    @Test public void testUnblockedUser() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("this is a message! ")
                                        .customHeader("buyer-name", "BuyerName"));

        assertFalse(processedMail.getMessage().getState().equals(MessageState.BLOCKED));
    }

}
