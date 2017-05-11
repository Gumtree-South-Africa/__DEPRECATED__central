package com.ecg.de.kleinanzeigen.replyts.buyeralias;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertTrue;

/**
 * User: acharton
 * Date: 11/12/13
 */
public class BuyerAliasIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule();


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


        assertTrue(processedMail.getOutboundMail().getFromName().equals("BuyerName über eBay Kleinanzeigen"));
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

        assertTrue(processedMail.getOutboundMail().getFromName().equals("SellerName über eBay Kleinanzeigen"));
    }



}
