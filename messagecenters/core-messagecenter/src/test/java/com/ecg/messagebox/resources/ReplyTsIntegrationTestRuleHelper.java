package com.ecg.messagebox.resources;

import com.ecg.replyts.core.runtime.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

public class ReplyTsIntegrationTestRuleHelper {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagebox_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_MP);
        properties.put("message.synchronizer.enabled", "true");
        properties.put("replyts.tenant", "mp");
        properties.put("hazelcast.password", "123");
        properties.put("hazelcast.port.increment", "true");
        properties.put("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        properties.put("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());
        properties.put("active.dc", "localhost");
        properties.put("region", "localhost");

        return properties;
    }
}