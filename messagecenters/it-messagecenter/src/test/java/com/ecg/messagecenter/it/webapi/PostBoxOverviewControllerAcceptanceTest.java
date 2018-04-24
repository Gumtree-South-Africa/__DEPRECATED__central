package com.ecg.messagecenter.it.webapi;

import javax.mail.MessagingException;

import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.junit.Test;

import com.ecg.messagecenter.it.persistence.Header;
import com.jayway.restassured.RestAssured;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxOverviewControllerAcceptanceTest extends BaseReplyTsIntegrationTest {

    @Test public void readConversation() {
        testRule.deliver(aNewMail().from("buyer1@buyer.com").to("seller1@seller.com").adId("232323")
                                        .plainBody("First contact from buyer."));

        testRule.waitForMail();

        RestAssured.given().expect().statusCode(200).body("body.conversations.size()", equalTo(1))
                        .body("body.conversations[0].email", equalTo("seller1@seller.com"))
                        .body("body.conversations[0].adId", equalTo("232323"))
                        .body("body.conversations[0].textShortTrimmed",
                                        equalTo("First contact from buyer."))
                        .get("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com");
    }

    @Test public void deleteConversation() {
        String conversationId = testRule.deliver(
                        aNewMail().from("buyer1@buyer.com").to("seller1@seller.com").adId("232323")
                                        .plainBody("First contact from buyer.")).getConversation()
                        .getId();

        testRule.waitForMail();

        RestAssured.given().expect().statusCode(200)
                        .delete("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com/conversations?ids="
                                        + conversationId);

        RestAssured.given().filter(new ResponseLoggingFilter()).expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");

    }

    @Test public void resetMessageCounter() throws MessagingException {
        testRule.deliver(aNewMail().from("buyer1@buyer.com").to("seller1@seller.com").adId("232323")
                                        .plainBody("First contact from first buyer."))
                        .getConversation().getId();
        testRule.waitForMail();
        testRule.deliver(aNewMail().from("buyer2@buyer.com").to("seller1@seller.com").adId("424242")
                                        .plainBody("First contact from second buyer."))
                        .getConversation().getId();
        String buyer2 = testRule.waitForMail().getFrom()[0].toString();
        testRule.deliver(aNewMail().from("seller1@seller.com").to(buyer2).adId("424242")
                                        .plainBody("A reply from the seller to the second buyer."))
                        .getConversation().getId();
        testRule.waitForMail();
        testRule.deliver(aNewMail().from("seller1@seller.com").to(buyer2).adId("424242")
                                        .header(Header.Robot.getValue(), "GTAU")
                                        .plainBody("Message from Robot to the second buyer."))
                        .getConversation().getId();
        testRule.waitForMail();

        RestAssured.given().expect().statusCode(200).body("body.numUnread", equalTo(1))
                        .get("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        RestAssured.given().expect().statusCode(200)
                        .put("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        // counter not reset in old counter mode
        RestAssured.given().expect().statusCode(200).body("body.numUnread", equalTo(1))
                        .get("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        // counter reset in new counter mode
        RestAssured.given().expect().statusCode(200).body("body.numUnread", equalTo(0))
                        .get("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/seller1@seller.com?newCounterMode=true");

        // Buyer

        RestAssured.given().expect().statusCode(200).body("body.numUnread", equalTo(1))
                        .get("http://localhost:" + testRule.getHttpPort()
                                        + "/ebayk-msgcenter/postboxes/buyer2@buyer.com");
    }
}
