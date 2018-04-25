package com.ecg.comaas.core.resultinspector.threshold;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;

public class ThresholdResultInspectorIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_GTUK), ScoringFilterFactory.class);

    @Before
    public void setUp() throws Exception {
        rule.registerConfig(ThresholdResultInspectorFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{'held':50, 'blocked':100}"));
    }

    @Test
    public void allowsMessageIfNoScore() throws Exception {
        MailInterceptor.ProcessedMail processed = rule.deliver(MailBuilder.aNewMail().adId("12").from("foo@bar.com").to("bar@foo.com").subject("as").htmlBody("asdf"));

        assertEquals(MessageState.SENT, processed.getMessage().getState());
    }

    @Test
    public void putsMessageOnBlockedIfFilterScoresExceeds() throws Exception {

        rule.registerConfig(ScoringFilterFactory.IDENTIFIER, JsonObjects.builder().attr("score", 100).build());

        MailInterceptor.ProcessedMail processed = rule.deliver(MailBuilder.aNewMail().adId("12").from("foo@bar.com").to("bar@foo.com").subject("as").htmlBody("asdf"));

        assertEquals(MessageState.BLOCKED, processed.getMessage().getState());

    }
}
