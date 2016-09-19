package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.messagebox.controllers.ReplyTsIntegrationTestRuleHelper.getTestRuleForOldModel;

public class OldConversationControllerAcceptanceTest extends BaseConversationControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForOldModel();

    @Test
    public void getConversation() throws Exception {
        getConversation(testRule, false);
    }

    @Test
    public void markConversationAsRead() throws Exception {
        markConversationAsRead(testRule);
    }
}