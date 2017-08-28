package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.webapi.requests.MessageCenterGetAdConversationRecipientsCommand;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

/**
 * Occasionally you will get errors like this:
 *
 java.lang.AssertionError: Expected 1 mails to arrive, but got 3
 at org.junit.Assert.fail(Assert.java:88)
 at com.ecg.replyts.integration.test.IntegrationTestRunner.waitForMessageArrival(IntegrationTestRunner.java:74)
 at com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.waitForMail(ReplyTsIntegrationTestRule.java:178)
 at com.ecg.messagecenter.webapi.AdConversationRecipientsControllerIntegrationTest.testGetBuyerContacts(AdConversationRecipientsControllerIntegrationTest.java:60)

 I don't know if this is a timing issue, as the assertion occurs within testRule.waitForMail();

 Rerunning the test sees this problem go away. Matt says he doesnt get it in his computer. Maybe it's a windows issue?

 *
 * Created by elvalencia on 24/07/2015.
 */
public class AdConversationRecipientsControllerIntegrationTest {
    private static String MESSAGE_CENTER_URI_BASE = "/message-center";

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("replyts.tenant", "kjca");
        properties.put("push-mobile.enabled","false");
        properties.put("persistence.strategy", "riak");
        properties.put("unread.count.cache.queue", "devull");

        return properties;
    }).get());

    /**
     * This email gets 3 emails sent to it, with the first 2 for AD_ID, and the 3rd email for another ad
     *
     * We should only get 2 buyer details for AD_ID
     *
     * @throws UnsupportedEncodingException
     */
    @Test
    public void testGetBuyerContacts() throws UnsupportedEncodingException {

        // there are a lot of characters that are actually valid in email addresses!
        // see: https://en.wikipedia.org/wiki/Email_address
        final String SELLER_EMAIL = "#!$%&'*+-/=?^_`{}|~@seller.com";
        final String AD_ID = "232323";

        // send first email
        MailInterceptor.ProcessedMail processedMail1 = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to(SELLER_EMAIL)
                        .adId(AD_ID)
                        .plainBody("First contact from buyer1.")
        );

        String conversationId1 = processedMail1.getConversation().getId();

        testRule.waitForMail();

        // send second email
        MailInterceptor.ProcessedMail processedMail2 = testRule.deliver(
                aNewMail()
                        .from("buyer2@buyer.com")
                        .to(SELLER_EMAIL)
                        .adId(AD_ID)
                        .plainBody("First contact from buyer2.")
        );

        String conversationId2 = processedMail2.getConversation().getId();

        testRule.waitForMail();


        // send an email for a different ad
        testRule.deliver(
                aNewMail()
                        .from("buyerForAnotherAd@buyer.com")
                        .to(SELLER_EMAIL)
                        .adId("55")
                        .plainBody("First contact from buyerForAnotherAd for a different ad.")
        );

        testRule.waitForMail();


        // our buyers will be ordered according to which has had a most recent updated conversation.
        // an conversation is updated if either seller/buyer sends a message.
        String urlEncodedEmail = URLEncoder.encode(SELLER_EMAIL, "UTF-8");

        MessageCenterGetAdConversationRecipientsCommand cmd
                = new MessageCenterGetAdConversationRecipientsCommand(urlEncodedEmail, AD_ID);

        String url = "http://localhost:" + testRule.getHttpPort() + MESSAGE_CENTER_URI_BASE + cmd.url();
        System.out.println("url: " + url);

        RestAssured.given()
                .when()
                    .get(url)
                .then()
                    .statusCode(200)
                    .body("body.adId", equalTo(AD_ID))
                    .body("body.buyerContacts.size()", equalTo(2))
                    .body("body.buyerContacts[0].name", equalTo(""))
                    .body("body.buyerContacts[0].email", equalTo("buyer2@buyer.com"))
                    .body("body.buyerContacts[0].conversationId", equalTo(conversationId2))
                    .body("body.buyerContacts[1].name", equalTo(""))
                    .body("body.buyerContacts[1].email", equalTo("buyer1@buyer.com"))
                    .body("body.buyerContacts[1].conversationId", equalTo(conversationId1));

    }


}
