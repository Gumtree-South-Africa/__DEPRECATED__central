package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.subethamail.wiser.WiserMessage;

import java.io.ByteArrayOutputStream;


import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class EmailWithDashAcceptanceTest {

    private static final int deliveryTimeoutSeconds = 45;

    // email addresses starting with dash
    private static final String BUYER_ADDRESS = "-buyer@buyer.com";
    private static final String SELLER_ADDRESS = "-seller@seller.com";

    private static MailBuilder MAIL_WITH_PREPENDING_DASH_IN_ADDRESSES = aNewMail()
            .from(BUYER_ADDRESS)
            .to(SELLER_ADDRESS)
            .header("X-CUST-user-id-buyer", "1")
            .header("X-CUST-user-id-seller", "2")
            .header("X-Message-Metadata", "message metadata")
            .adId("232323")
            .plainBody("first contact from buyer");

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(deliveryTimeoutSeconds, ES_ENABLED);

    @Test
    public void rtsSendAndReplyMessagesOnAddressesWithDash() throws Exception {

        // sends the email
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MAIL_WITH_PREPENDING_DASH_IN_ADDRESSES.build().writeTo(output);
        rule.getMailSender().sendMail(output.toByteArray());

        // wait for listener to be triggered - this means the mail is sent & stored properly
        rule.getMailInterceptor().awaitMails(1, deliveryTimeoutSeconds);

        WiserMessage anonymizedAsq = rule.waitForWiserMail();
        String anonymousAsqSender = anonymizedAsq.getEnvelopeSender();
        assertThat(anonymizedAsq.getEnvelopeReceiver(), is(SELLER_ADDRESS));
        assertThat(anonymousAsqSender, not(BUYER_ADDRESS));
    }
}


