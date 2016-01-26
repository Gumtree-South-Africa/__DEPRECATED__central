package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: gdibella
 * Date: 9/5/13
 */
public class MultiDomainAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void buyerDomainIntialReply() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail output =
                rule.deliver(MailBuilder.aNewMail().from("foo@bar.com")
                        .to("bar123123@foo.com").subject("multi domain test").adId("123123").htmlBody("foo")
                        .customHeader("buyer_domain", "test-platform2.com")
                        .customHeader("seller_domain", "test-platform3.com"));

        assertEquals("test-platform2.com", output.getConversation().getCustomValues().get("buyer_domain"));
        assertEquals("test-platform3.com", output.getConversation().getCustomValues().get("seller_domain"));
        assertTrue(output.getOutboundMail().getFrom().endsWith("test-platform2.com"));
    }

    @Test
    public void sellerDomainResponse() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail outbound =
                rule.deliver(MailBuilder.aNewMail().from("f123oo@bar.com")
                        .to("bar@foo.com").subject("multi domain test").adId("123").htmlBody("foo")
                        .customHeader("buyer_domain", "test-platform2.com")
                        .customHeader("seller_domain", "test-platform3.com"));


        AwaitMailSentProcessedListener.ProcessedMail response =
                rule.deliver(MailBuilder.aNewMail().from("seller@bar.com")
                        .to(outbound.getOutboundMail().getFrom()).htmlBody("foo"));

        assertTrue(response.getOutboundMail().getFrom().endsWith("test-platform3.com"));
    }

    @Test
    public void noDomainsSent() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail output =
                rule.deliver(MailBuilder.aNewMail()
                        .from("f23oo1@bar.com")
                        .to("bar1@foo.com")
                        .subject("multi domain test")
                        .adId("123")
                        .htmlBody("foo"));

        assertFalse(output.getConversation().getCustomValues().containsKey("buyer_domain"));
        assertFalse(output.getConversation().getCustomValues().containsKey("seller_domain"));
        assertTrue(output.getOutboundMail().getFrom().endsWith("test-platform.com"));
    }

    @Test
    public void badDomainsSent() throws Exception {
        AwaitMailSentProcessedListener.ProcessedMail output =
                rule.deliver(MailBuilder.aNewMail().from("foo13@bar.com")
                        .to("bar12@foo.com")
                        .subject("multi domain test")
                        .adId("123").htmlBody("foo")
                        .customHeader("buyer_domain", "fdafdsfadsfads.com")
                        .customHeader("seller_domain", "98ewq9800.it"));

        assertTrue(output.getOutboundMail().getFrom().endsWith("test-platform.com"));
    }
}
