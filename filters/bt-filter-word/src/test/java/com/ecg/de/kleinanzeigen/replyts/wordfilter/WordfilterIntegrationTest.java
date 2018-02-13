package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.de.kleinanzeigen.replyts.wordfilter.Wordfilter.CATEGORY_ID;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;

public class WordfilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

    @Before
    public void setUp() {
        testRule.registerConfig(WordfilterFactory.class, (ObjectNode) JsonObjects.parse("" +
                "{'ignoreQuotedRegexps':true,'rules': [{'regexp': 'badword', 'score':2000}," +
                "{'regexp': 'meanword', 'score':12000}," +
                "{'regexp': 'badcategoryword', 'score':6000 , 'categoryIds': ['c218', 'c45556565']}]}"));

    }

    @Test
    public void wordfilterFiresOnPatternHit() {
        MailInterceptor.ProcessedMail processed = testRule.deliver(
                aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a <b>badword</b>! ")
        );
        testRule.waitForMail();

        assertEquals(1, processed.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void wordfilterFiresOnPatternHitWithinCategory() {
        MailInterceptor.ProcessedMail processed = testRule.deliver(
                aNewMail()
                        .customHeader(CATEGORY_ID, "c218")
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a <b>badcategoryword</b>! ")
        );
        testRule.waitForMail();

        assertEquals(1, processed.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void ignoresQuotedRegularExpressions() {
        MailInterceptor.ProcessedMail processed = testRule.deliver(
                aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a <b>badword</b>! ")
        );
        testRule.waitForMail();

        processed = testRule.deliver(
                aNewMail()
                        .from("bar@foo.com")
                        .to(processed.getOutboundMail().getFrom())
                        .htmlBody("this is badword number two")
        );
        testRule.waitForMail();

        ProcessingFeedback feedback = processed.getMessage().getProcessingFeedback().get(0);
        assertEquals("badword", feedback.getUiHint());
        assertEquals(0l, feedback.getScore().longValue());
    }

    @Test
    public void countsUnquotedRegularExpressions() {
        MailInterceptor.ProcessedMail processed = testRule.deliver(
                aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a <b>badword</b>! ")
        );
        testRule.waitForMail();

        processed = testRule.deliver(
                aNewMail()
                        .from("bar@foo.com")
                        .to(processed.getOutboundMail().getFrom())
                        .htmlBody("this is meanword")
        );
        testRule.waitForMail();

        ProcessingFeedback feedback = processed.getMessage().getProcessingFeedback().get(0);
        assertEquals("meanword", feedback.getUiHint());
        assertEquals(12000l, feedback.getScore().longValue());
    }
}

