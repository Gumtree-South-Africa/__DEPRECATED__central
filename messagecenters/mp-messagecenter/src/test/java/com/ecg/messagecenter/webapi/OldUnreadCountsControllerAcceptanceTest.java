package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.messagebox.controllers.ReplyTsIntegrationTestRuleHelper.getTestRuleForOldModel;

public class OldUnreadCountsControllerAcceptanceTest extends BaseUnreadCountsControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForOldModel();

    @Test
    public void getPostBoxUnreadCounts() {
        getPostBoxUnreadCounts(testRule);
    }
}