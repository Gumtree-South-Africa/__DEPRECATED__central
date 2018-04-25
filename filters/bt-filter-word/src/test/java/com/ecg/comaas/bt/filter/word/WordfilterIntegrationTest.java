package com.ecg.comaas.bt.filter.word;

import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MX;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;

public class WordfilterIntegrationTest {
    private static final String CATEGORY_ID = "categoryid";

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_MX));

    @Before
    public void setUp() throws Exception {
        rule.registerConfig(WordfilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("" +
          "{'ignoreQuotedRegexps':true,'rules': [{'regexp': 'badword', 'score':2000}," +
          "{'regexp': 'meanword', 'score':12000}," +
          "{'regexp': 'badcategoryword', 'score':6000 , 'categoryIds': ['c218', 'c45556565']}]}"));
    }

    @Test
    public void wordfilterFiresOnPatternHit() throws Exception {
        MailInterceptor.ProcessedMail processedMail = rule.deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());

    }

    @Test
    public void wordfilterFiresOnPatternHitWithinCategory() throws Exception {
        MailInterceptor.ProcessedMail processedMail = rule.deliver(aNewMail().customHeader(CATEGORY_ID, "c218").adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badcategoryword</b>! "));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void ignoresQuotedRegularExpressions() {
        MailInterceptor.ProcessedMail processedMail = rule.deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        processedMail = rule.deliver(aNewMail().from("bar@foo.com").to(processedMail.getOutboundMail().getFrom()).htmlBody("this is badword number two"));

        ProcessingFeedback feedback = processedMail.getMessage().getProcessingFeedback().get(0);
        assertEquals("badword",feedback.getUiHint());
        assertEquals(0l, feedback.getScore().longValue());
    }

    @Test
    public void countsUnquotedRegularExpressions() {
        MailInterceptor.ProcessedMail processedMail = rule.deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        processedMail = rule.deliver(aNewMail().from("bar@foo.com").to(processedMail.getOutboundMail().getFrom()).htmlBody("this is meanword"));

        ProcessingFeedback feedback = processedMail.getMessage().getProcessingFeedback().get(0);
        assertEquals("meanword",feedback.getUiHint());
        assertEquals(12000l, feedback.getScore().longValue());
    }
}
