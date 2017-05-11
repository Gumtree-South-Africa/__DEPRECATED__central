package com.ecg.de.mobile.replyts.uniqueid;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by beckart on 09.03.15.
 */
public class MailUniqueIdIntegrationTest {
    public static final String X_MOBILEDE_BUYER_ID = "X-MOBILEDE-BUYER-ID";

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("replyts.uniqueid.ignoredBuyerAddresses", "foo@Foo.com , a@b.com");
        properties.put("replyts.uniqueid.order", "1");

        return properties;
    }).get());

    private MailBuilder createInitialMailFromBuyer() {
        return MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("mobile.de <noreply@team.mobile.de>")
                .header("Reply-To", "buyer <buyer@team.mobile.de>")
                .header("X-CUST-SELLER_TYPE", "DEALER")
                .to("Seller <seller@example.com>");
    }

    private MailBuilder createMail(String from, String to) {
        return MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from(from)
                .to(to);
    }

    @Test
    public void testFromBuyerToSeller() {

        MailBuilder mailBuilder = createInitialMailFromBuyer();
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        String uniqueBuyerId = new UniqueIdGenerator("wz1239yjdsfhqyOEedd").generateUniqueBuyerId("buyer@team.mobile.de");

        assertEquals(uniqueBuyerId, processedMail.getOutboundMail().getUniqueHeader(X_MOBILEDE_BUYER_ID));
    }


    @Test
    public void testFromBuyerToSellerOnIgnoreList() {


        MailBuilder mail = createMail("foo@foo.com", "myseller@team.mobile.de");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mail);

        assertNull(processedMail.getOutboundMail().getUniqueHeader(X_MOBILEDE_BUYER_ID));
    }

    @Test
    public void testFromSellerToBuyer() {

        // First mail buyer to seller

        MailBuilder initialMailFromBuyer = createInitialMailFromBuyer();

        MailInterceptor.ProcessedMail processedInitialMailFromBuyer = replyTsIntegrationTestRule.deliver(initialMailFromBuyer);


        // Second mail seller to buyer

        MailBuilder mailFromSeller = createMail("myseller@team.mobile.de", processedInitialMailFromBuyer.getOutboundMail().getFrom());

        MailInterceptor.ProcessedMail processedMailFromSeller = replyTsIntegrationTestRule.deliver(mailFromSeller);

        assertNull(processedMailFromSeller.getOutboundMail().getUniqueHeader(X_MOBILEDE_BUYER_ID));


        // Third mail buyer to seller

        MailBuilder secondMailFromBuyer = createMail("mybuyer@team.mobile.de", processedMailFromSeller.getOutboundMail().getFrom());

        MailInterceptor.ProcessedMail processedSecondMailFromBuyer = replyTsIntegrationTestRule.deliver(secondMailFromBuyer);


        // check buyer id

        String uniqueBuyerId = new UniqueIdGenerator("wz1239yjdsfhqyOEedd").generateUniqueBuyerId("buyer@team.mobile.de");

        assertEquals(uniqueBuyerId, processedSecondMailFromBuyer.getOutboundMail().getUniqueHeader(X_MOBILEDE_BUYER_ID));


    }



}
