package com.ecg.comaas.core.filter.volume;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;

public class VolumeFilterIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_EBAYK), ES_ENABLED)
            .addCassandraSchema("core_volume_filter.cql");

    @Test
    public void multipleRegistrations() {
        rule.deleteConfig(registerVolumeConfig());
        registerVolumeConfig();

        MailInterceptor.ProcessedMail response = rule.deliver(
                MailBuilder.aNewMail().adId("123").from("whatever@example.com").to("bar@foo.com").htmlBody("oobar")
        );
        assertEquals(MessageState.SENT, response.getMessage().getState());
        assertEquals(0, response.getMessage().getProcessingFeedback().size());
    }

    private Configuration.ConfigurationId registerVolumeConfig() {
        final ArrayNode add = JsonObjects.newJsonArray().add(JsonObjects.builder().attr("allowance", "1").attr("perTimeValue", "1").attr("perTimeUnit", "DAYS").attr("score", "100").build());
        final ObjectNode rules1 = JsonObjects.builder().attr("rules", add).build();

        return rule.registerConfig(VolumeFilterFactory.IDENTIFIER, rules1);
    }

    @Test
    public void violatesQuota() throws Exception {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"HOURS\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 1, \"perTimeUnit\": \"DAYS\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));


        String from = "foo" + System.currentTimeMillis() + "@bar.com";
        for (int i = 0; i < 3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            rule.waitUntilIndexedInEs(response);
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void skipsQuotaViolation() throws Exception {
        rule.registerConfig(VolumeFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{\n" +
                "    rules: [\n" +
                "        {\"allowance\": 3, \"perTimeValue\": 1, \"perTimeUnit\": \"MINUTES\", \"score\": 100},\n" +
                "        {\"allowance\": 20, \"perTimeValue\": 10, \"perTimeUnit\": \"MINUTES\", \"score\": 200}\n" +
                "    ]\n" +
                " }"));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";
        for (int i = 0; i < 2; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
            assertEquals(MessageState.SENT, response.getMessage().getState());
            rule.waitUntilIndexedInEs(response);
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("oobar"));
        assertEquals(MessageState.SENT, response.getMessage().getState());
    }
}
