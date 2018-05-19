package com.ecg.messagecenter.it.webapi;

import com.ecg.messagecenter.it.cleanup.TextCleaner;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

/**
 * Created by jaludden on 17/11/15.
 */
public class BaseReplyTsIntegrationTest {
    @Rule public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties()).addCassandraSchema("cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_IT);
        properties.put("persistence.strategy", "cassandra");
        properties.put("persistence.cassandra.conversation.class", "com.ecg.messagecenter.it.persistence.ConversationThread");
        return properties;
    }

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
