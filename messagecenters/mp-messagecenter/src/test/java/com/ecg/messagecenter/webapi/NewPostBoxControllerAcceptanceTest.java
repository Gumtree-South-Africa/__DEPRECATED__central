package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.messagebox.controllers.ReplyTsIntegrationTestRuleHelper.getTestRuleForNewModel;
import static org.joda.time.DateTime.now;

public class NewPostBoxControllerAcceptanceTest extends BasePostBoxControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForNewModel();

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getPostBox() {
        getPostBox(testRule, true);
    }

    @Test
    public void deleteConversations() {
        deleteConversations(testRule);
    }
}