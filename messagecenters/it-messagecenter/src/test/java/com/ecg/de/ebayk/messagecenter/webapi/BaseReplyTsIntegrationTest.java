package com.ecg.de.ebayk.messagecenter.webapi;

import com.ecg.de.ebayk.messagecenter.cleanup.TextCleaner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;

/**
 * Created by jaludden on 17/11/15.
 */
public class BaseReplyTsIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

    @BeforeClass public static void setUp() {
        System.setProperty("api.host", "localhost");
    }

    @Before public void setUpTextCleaner() {
        TextCleaner.setInstance(new TextCleaner.GumtreeTextCleaner());
    }

    @After public void tearDownTextCleaner() {
        TextCleaner.setInstance(null);
    }

}
