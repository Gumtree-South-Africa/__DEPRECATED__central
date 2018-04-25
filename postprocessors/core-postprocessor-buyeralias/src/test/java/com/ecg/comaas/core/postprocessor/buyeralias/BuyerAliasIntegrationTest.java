package com.ecg.comaas.core.postprocessor.buyeralias;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.junit.Assert.assertEquals;

public class BuyerAliasIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_GTUK));

    @Test
    public void buyerAliasAppended() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a message! ")
                        .customHeader("buyer-name", "BuyerName")
                        .customHeader("seller-name", "SellerName")
                );

        assertEquals("BuyerName über eBay Kleinanzeigen", processedMail.getOutboundMail().getFromName());
    }

    @Test
    public void sellerAliasAppended() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a message! ")
                        .customHeader("buyer-name", "BuyerName")
                        .customHeader("seller-name", "SellerName")
                );

        processedMail = replyTsIntegrationTestRule.deliver(aNewMail().to(processedMail.getOutboundMail().getFrom()).from("foo@bar.com").htmlBody("replyFromSeller"));

        assertEquals("SellerName über eBay Kleinanzeigen", processedMail.getOutboundMail().getFromName());
    }
}
