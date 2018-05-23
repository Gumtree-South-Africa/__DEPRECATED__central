package com.ecg.comaas.gtuk.filter.volume;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;

public class GumtreeVolumeFilterIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_GTUK));

    @Test
    public void testMultipleRegistrations() {
        rule.deleteConfig(registerVolumeConfig());
        registerVolumeConfig();

        MailInterceptor.ProcessedMail response = rule.deliver(
                MailBuilder.aNewMail().adId("123").from("whatever@example.com").to("bar@foo.com").htmlBody("oobar")
        );
        assertEquals(MessageState.SENT, response.getMessage().getState());
        assertEquals(0, response.getMessage().getProcessingFeedback().size());
    }

    private Configuration.ConfigurationId registerVolumeConfig() {
        return rule.registerConfig(GumtreeVolumeFilterFactory.IDENTIFIER,
                (ObjectNode) JsonObjects.parse("{\n" +
                        "      \"result\" : \"HOLD\",\n" +
                        "      \"seconds\" : 3600,\n" +
                        "      \"exceeding\" : true,\n" +
                        "      \"messageState\" : null,\n" +
                        "      \"exemptedCategories\" : [ ],\n" +
                        "      \"messages\" : 1,\n" +
                        "      \"state\" : \"ENABLED\",\n" +
                        "      \"priority\" : 100,\n" +
                        "      \"version\" : \"1.7.23.BUILD\",\n" +
                        "      \"filterField\" : \"EMAIL\",\n" +
                        "      \"whitelistSeconds\" : 86400\n" +
                        "    }"), 100);
    }
}
