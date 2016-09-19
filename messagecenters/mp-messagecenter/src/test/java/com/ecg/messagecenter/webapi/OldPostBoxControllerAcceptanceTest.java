package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.messagebox.controllers.ReplyTsIntegrationTestRuleHelper.getTestRuleForOldModel;

public class OldPostBoxControllerAcceptanceTest extends BasePostBoxControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForOldModel();

    @Test
    public void getPostBox() {
        getPostBox(testRule, false);
    }

    @Test
    public void deleteConversations() {
        deleteConversations(testRule);
    }
}