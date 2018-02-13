package com.ebay.ecg.australia.replyts.fromname;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mdarapour
 */
public class FromNamePostprocessorIT {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @BeforeClass
    public static void load() {
        System.setProperty("replyts.from-name.header.buyer", "buyer-name");
        System.setProperty("replyts.from-name.header.seller", "seller-name");
        System.setProperty("replyts.from-name.plugin.order", "250");
    }

    @Test
    public void updatesFromWhenBuyerToSeller() throws MessagingException {
        Mail mail = rule.deliver(MailBuilder
                .aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")
                .customHeader("buyer-name", "Fooler")
                .customHeader("seller-name", "Barber")).getOutboundMail();

        assertNotNull("Headers must include 'From'", mail.getHeaders("From"));
        assertEquals("Expecting one 'From' in the header", 1, mail.getHeaders("From").size());
        assertEquals(String.format("Fooler <%s>", mail.getFrom()), mail.getHeaders("From").get(0));
    }

    @Test
    public void updatesFromWhenSellerToBuyer() throws MessagingException {
        rule.deliver(MailBuilder
                .aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")
                .customHeader("buyer-name", "Fooler")
                .customHeader("seller-name", "Barber"));

        MimeMessage anonymizedInitialMail = rule.waitForMail();
        String anonymizedBuyer = anonymizedInitialMail.getFrom()[0].toString();
        Mail mail = rule.deliver(MailBuilder
                .aNewMail()
                .from("seller@bar.com")
                .to(anonymizedBuyer)
                .adId("213")
                .htmlBody("hello buyer")
                .customHeader("buyer-name", "Fooler")
                .customHeader("seller-name", "Barber")).getOutboundMail();

        assertNotNull("Headers must include 'From'", mail.getHeaders("From"));
        assertEquals("Expecting one 'From' in the header", 1, mail.getHeaders("From").size());
        assertEquals(String.format("Barber <%s>", mail.getFrom()), mail.getHeaders("From").get(0));
    }
}
