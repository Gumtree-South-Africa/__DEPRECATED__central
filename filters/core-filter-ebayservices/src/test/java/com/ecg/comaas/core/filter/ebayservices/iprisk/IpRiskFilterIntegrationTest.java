package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ecg.comaas.core.filter.ebayservices.MockStateHolder;
import com.ecg.comaas.core.filter.ebayservices.TestPropertiesUtils;
import com.ecg.comaas.core.filter.ebayservices.userstate.UserStateFilterIntegrationTest;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static org.assertj.core.api.Assertions.assertThat;

public class IpRiskFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule(TestPropertiesUtils.getProperties(TENANT_EBAYK));

    @BeforeClass
    public static void initMockInterceptor() {
        MockStateHolder.loadFromFile(UserStateFilterIntegrationTest.class.getResourceAsStream("/mockingInterceptor.config"));
    }

    @Before
    public void setUp() {
        ObjectNode jsonNode = JsonObjects.builder().attr("GOOD", 20).attr("BAD", 20).attr("MEDIUM_BAD", 20).attr("VERY_BAD", 20).build();
        itRule.registerConfig(IpRiskFilterFactory.IDENTIFIER, jsonNode);
    }

    @Test
    public void whenIpHitsFilter_shouldGenerateProperFeedback() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .header("X-CUST-IP", "194.50.69.177")
                .adId("123")
                .from("buyer@test.de")
                .to("seller@test.de")
                .htmlBody("hello world!")
        );

        List<ProcessingFeedback> actualFeedback = processedMail.getMessage().getProcessingFeedback();

        assertThat(actualFeedback).hasSize(1);
        assertThat(actualFeedback.get(0).getScore()).isEqualTo(20);
    }
}
