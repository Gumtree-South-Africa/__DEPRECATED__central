package com.ecg.messagebox.controllers;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ThrowAwayAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = new Properties();
        properties.put(TENANT, TENANT_EBAYK);
        properties.put("replyts.tenant", "ebayk");
        properties.put("message.synchronizer.enabled", "false");
        properties.put("hazelcast.password", "123");
        properties.put("hazelcast.port.increment", "true");
        properties.put("active.dc", "localhost");
        properties.put("region", "localhost");
        properties.put("persistence.cassandra.conversation.class", "com.ecg.messagecenter.ebayk.persistence.ConversationThread");
        return properties;
    }

}
