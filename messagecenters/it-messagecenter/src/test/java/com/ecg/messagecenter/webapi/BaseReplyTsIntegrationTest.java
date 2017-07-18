package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.cleanup.TextCleaner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;

import java.util.Properties;

/**
 * Created by jaludden on 17/11/15.
 */
public class BaseReplyTsIntegrationTest {
    private final Properties testProperties = new Properties() {{
        put("replyts.tenant", "it");
        put("persistence.strategy", "riak");
    }};

    @Rule public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties).addCassandraSchema("cassandra_messagecenter_schema.cql");

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
