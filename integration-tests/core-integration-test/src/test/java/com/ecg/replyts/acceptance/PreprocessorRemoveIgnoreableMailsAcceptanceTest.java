package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;

public class PreprocessorRemoveIgnoreableMailsAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule();

    @Test
    public void rtsDoesNotProcessAutomatedMail() {
        MailInterceptor.ProcessedMail outcome = replyTsIntegrationTestRule.deliver(
                aNewMail()
                        .from("a@b.com")
                        .to("b@c.com")
                        .adId("as")
                        .header("precedence", "junk")
                        .plainBody("foobar")
        );

        assertEquals(ConversationState.ACTIVE, outcome.getConversation().getState());
        assertEquals(MessageState.IGNORED, outcome.getMessage().getState());
        replyTsIntegrationTestRule.assertNoMailArrives();
    }


    @Test
    public void rtsDoesNotSendOrphanedMails() {
        MailInterceptor.ProcessedMail outcome = replyTsIntegrationTestRule.deliver(
                aNewMail()
                        .from("a@b.com")
                        .to("Buyer.00000@test-platform.com")
                        .adId("a")
                        .plainBody("foobar")
        );

        assertEquals(ConversationState.DEAD_ON_ARRIVAL, outcome.getConversation().getState());
        assertEquals(MessageState.ORPHANED, outcome.getMessage().getState());
        replyTsIntegrationTestRule.assertNoMailArrives();
    }

}
