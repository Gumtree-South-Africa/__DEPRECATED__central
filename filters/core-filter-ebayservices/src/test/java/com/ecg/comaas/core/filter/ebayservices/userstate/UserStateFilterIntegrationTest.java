package com.ecg.comaas.core.filter.ebayservices.userstate;

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

public class UserStateFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule(TestPropertiesUtils.getProperties(TENANT_EBAYK));

    @Before
    public void setUp() throws Exception {
        ObjectNode jsonNode = JsonObjects.builder().attr("UNKNOWN", 1).attr("CONFIRMED", 1).attr("SUSPENDED", 1).build();
        itRule.registerConfig(UserStateFilterFactory.IDENTIFIER, jsonNode);
    }

    @Test
    public void whenUserStateDoesNotHitFilter_shouldGenerateEmptyFeedback() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .adId("123")
                .from("tester@ebay.de")
                .to("seller@test.de")
                .htmlBody("hello world!")
        );

        List<ProcessingFeedback> actualFeedback = processedMail.getMessage().getProcessingFeedback();

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenUserStateHitsFilter_shouldGenerateProperFeedback() throws Exception {
        MailInterceptor.ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .adId("123")
                .from("matthias.huttar@gmx.de")
                .to("seller@test.de")
                .htmlBody("hello world!")
        );

        List<ProcessingFeedback> actualFeedback = processedMail.getMessage().getProcessingFeedback();

        assertThat(actualFeedback).hasSize(1);
        assertThat(actualFeedback.get(0).getScore()).isEqualTo(1);
    }
}
