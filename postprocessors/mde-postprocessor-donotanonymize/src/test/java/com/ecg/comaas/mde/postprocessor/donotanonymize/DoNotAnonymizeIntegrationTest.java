package com.ecg.comaas.mde.postprocessor.donotanonymize;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class DoNotAnonymizeIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_MDE));

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


        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

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


        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

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


        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

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


        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        assertFalse("noreply@team.mobile.de".equals(processedMail.getOutboundMail().getFrom()));

    }

}
