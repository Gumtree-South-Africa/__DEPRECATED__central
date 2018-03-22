package com.ecg.messagebox.controllers;

import com.ecg.replyts.core.runtime.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;

import java.util.Properties;
import java.util.function.Supplier;

public class ReplyTsIntegrationTestRuleHelper {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("message.synchronizer.enabled", "true");
        properties.put("replyts.tenant", "mp");
        properties.put("hazelcast.password", "123");
        properties.put("hazelcast.port.increment", "true");
        properties.put("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        properties.put("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());
        properties.put("active.dc", "localhost");
        properties.put("region", "localhost");

        return properties;
    }).get(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagebox_schema.cql");
}