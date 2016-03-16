package com.ecg.de.mobile.replyts.deanonymize;

import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class MailDoNotAnonymizeIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule();


    @Test
    public void testDoNotAnonymize() {
        MailBuilder mailBuilder = MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("mobile.de <noreply@team.mobile.de>")
                .header("Reply-To", "buyer <buyer@team.mobile.de>")
                .header("X-DO-NOT-ANONYMIZE", "J2y$idmEqLo2yyDyope")
                .to("Seller <seller@example.com>");


        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        assertEquals("noreply@team.mobile.de", processedMail.getOutboundMail().getFrom());
        assertEquals("mobile.de", processedMail.getOutboundMail().getFromName());
        assertEquals("buyer <buyer@team.mobile.de>", processedMail.getOutboundMail().getUniqueHeader("Reply-To"));

    }

    @Test
    public void testDoNotAnonymizeWithoutPersonalName() {
        MailBuilder mailBuilder = MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("noreply@team.mobile.de")
                .header("Reply-To", "buyer@team.mobile.de")
                .header("X-DO-NOT-ANONYMIZE", "J2y$idmEqLo2yyDyope")
                .to("seller@example.com");


        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        assertEquals("noreply@team.mobile.de", processedMail.getOutboundMail().getFrom());
        assertNull(processedMail.getOutboundMail().getFromName());
        assertEquals("buyer@team.mobile.de", processedMail.getOutboundMail().getUniqueHeader("Reply-To"));

    }

    @Test
    public void testDoNotAnonymizeNoReplyTo() {
        MailBuilder mailBuilder = MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("noreply@team.mobile.de")
                .header("X-DO-NOT-ANONYMIZE", "J2y$idmEqLo2yyDyope")
                .to("seller@example.com");


        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        assertEquals("noreply@team.mobile.de", processedMail.getOutboundMail().getFrom());
        assertNull(processedMail.getOutboundMail().getFromName());
        assertNull(processedMail.getOutboundMail().getUniqueHeader("Reply-To"));

    }

    @Test
    public void testAnonymize() {
        MailBuilder mailBuilder = MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("noreply@team.mobile.de")
                .to("seller@example.com");


        AwaitMailSentProcessedListener.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        assertFalse("noreply@team.mobile.de".equals(processedMail.getOutboundMail().getFrom()));

    }

}
