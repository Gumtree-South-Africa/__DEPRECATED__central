package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.ecg.de.kleinanzeigen.replyts.wordfilter.Wordfilter.CATEGORY_ID;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.junit.Assert.assertEquals;

public class WordfilterIntegrationTest {

    private final Properties testProperties = new Properties() {{
        put("persistence.strategy", "riak");
    }};

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(testProperties, null, 20, ES_ENABLED);



    @Before
    public void setUp() throws Exception {
        replyTsIntegrationTestRule.registerConfig(WordfilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("" +
                "{'ignoreQuotedRegexps':true,'rules': [" +
                "{'regexp': 'badword', 'score':2000}," +
                "{'regexp': 'meanword', 'score':12000}," +
                "{'regexp': 'badcategoryword', 'score':6000 , 'categoryIds': ['c218', 'c45556565']}]}"));

    }


    @Test
    public void wordfilterFiresOnPatternHit() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());

    }

    @Test
    public void wordfilterFiresOnPatternHitWithinCategory() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail().customHeader(CATEGORY_ID, "c218").adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badcategoryword</b>! "));

        assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());

    }

    @Test
    public void ignoresQuotedRegularExpressions() {

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        processedMail = replyTsIntegrationTestRule.deliver(aNewMail().from("bar@foo.com").to(processedMail.getOutboundMail().getFrom()).htmlBody("this is badword number two"));

        ProcessingFeedback feedback = processedMail.getMessage().getProcessingFeedback().get(0);
        assertEquals("badword", feedback.getUiHint());
        assertEquals(0L, feedback.getScore().longValue());
    }

    @Test
    public void countsUnquotedRegularExpressions() {

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("this is a <b>badword</b>! "));

        processedMail = replyTsIntegrationTestRule.deliver(aNewMail().from("bar@foo.com").to(processedMail.getOutboundMail().getFrom()).htmlBody("this is meanword"));

        ProcessingFeedback feedback = processedMail.getMessage().getProcessingFeedback().get(0);
        assertEquals("meanword", feedback.getUiHint());
        assertEquals(12000L, feedback.getScore().longValue());
    }

    @Test
    public void followUpsIgnored() throws Exception {
        replyTsIntegrationTestRule.getConfigClient()
                .listConfigurations()
                .forEach(configuration -> replyTsIntegrationTestRule.deleteConfig(configuration.getConfigurationId()));

        replyTsIntegrationTestRule.registerConfig(WordfilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse(
                "{'ignoreFollowUps':true,'rules': [{'regexp': 'badword', 'score':2000}]}")
        );

        MailBuilder initialReply = aNewMail().uniqueIdentifier("first").adId("1234").from("foo@bar.com").to("bar@foo.com").htmlBody("No problems here");
        MailInterceptor.ProcessedMail initialMail = replyTsIntegrationTestRule.deliver(initialReply);
        assertEquals(0, initialMail.getMessage().getProcessingFeedback().size());

        MailBuilder followUp = aNewMail()
                .from(initialMail.getConversation().getSellerId())
                .to(initialMail.getOutboundMail().getFrom())
                .htmlBody("uh oh. badword here. but should be ignored 'cause it's a follow-up");
        MailInterceptor.ProcessedMail followUpMail = replyTsIntegrationTestRule.deliver(followUp);
        assertEquals(0, followUpMail.getMessage().getProcessingFeedback().size());
    }
}

