package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.TestPropertiesUtils;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static org.assertj.core.api.Assertions.assertThat;

public class Ip2CountryFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule(TestPropertiesUtils.getProperties(TENANT_EBAYK));

    @Before
    public void setUp() throws Exception {
        ObjectNode config = JsonObjects.builder().attr("DEFAULT", 50).attr("DE", 0).attr("NL", 200).build();
        itRule.registerConfig(Ip2CountryFilterFactory.IDENTIFIER, config);
    }

    @Test
    public void whenCountryHitsFilter_shouldGenerateProperFeedback() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .header("X-CUST-IP", "91.211.73.240")
                .adId("123")
                .from("buyer@test.de")
                .to("seller@test.de")
                .htmlBody("hello world!")
        );
        List<ProcessingFeedback> actualFeedback = processedMail.getMessage().getProcessingFeedback();

        assertThat(actualFeedback).hasSize(1);
        assertThat(actualFeedback.get(0).getScore()).isEqualTo(200);
    }
}
