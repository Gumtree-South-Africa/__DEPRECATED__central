package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.Header;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxOverviewControllerAcceptanceTest {
    private final Properties testProperties = new Properties() {{
        put("persistence.strategy", "riak");
    }};

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties, null, 20, ES_ENABLED);

    @Test
    public void readConversation() {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].email", equalTo("seller1@seller.com"))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");
    }

    @Test
    public void deleteConversation() {
        String conversationId = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        ).getConversation().getId();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com/conversations?ids=" + conversationId);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");

    }

    @Test
    public void resetMessageCounter() throws MessagingException {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from first buyer.")
        ).getConversation().getId();
        testRule.waitForMail();
        testRule.deliver(
                aNewMail()
                        .from("buyer2@buyer.com")
                        .to("seller1@seller.com")
                        .adId("424242")
                        .plainBody("First contact from second buyer.")
        ).getConversation().getId();
        String buyer2 = testRule.waitForMail().getFrom()[0].toString();
        testRule.deliver(
                aNewMail()
                        .from("seller1@seller.com")
                        .to(buyer2)
                        .adId("424242")
                        .plainBody("A reply from the seller to the second buyer.")
        ).getConversation().getId();
        testRule.waitForMail();
        testRule.deliver(
                aNewMail()
                        .from("seller1@seller.com")
                        .to(buyer2)
                        .adId("424242")
                        .header(Header.Robot.getValue(), "GTAU")
                        .plainBody("Message from Robot to the second buyer.")
        ).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        // counter not reset in old counter mode
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");

        // counter reset in new counter mode
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com?newCounterMode=true");

        // Buyer

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer2@buyer.com");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1)) //TODO: There must be still one unread message in the conversation, defect!
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer2@buyer.com?robotEnabled=false");
    }

    @Test
    public void readConversationWithRobotMessages() throws MessagingException {
        testRule.deliver(
                aNewMail()
                        .from("buyer11@buyer.com")
                        .to("seller11@seller.com")
                        .adId("23232323")
                        .plainBody("First contact from buyer.")
        );
        String anonymizedBuyer = testRule.waitForMail().getFrom()[0].toString();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .body("body.conversations[0].buyerId", equalTo("buyer11@buyer.com"))
                .body("body.conversations[0].sellerId", equalTo("seller11@seller.com"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com?robotEnabled=false");

        testRule.deliver(
                aNewMail()
                        .from("seller11@buyer.com")
                        .to(anonymizedBuyer)
                        .adId("23232323")
                        .header(Header.Robot.getValue(), "GTAU")
                        .plainBody("A message from Gumtree Robot.")
        );
        testRule.waitForMail();

        // Seller View

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com");

        // Called by API
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com?robotEnabled=false");

        // Buyer View

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("A message from Gumtree Robot."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer11@buyer.com");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true)) //TODO users would still get the robot message counter
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer11@buyer.com?robotEnabled=false");
    }

    @Test
    public void conversationShouldHaveOfferIdWhenTheLastMessageIsOffer() throws MessagingException {
        testRule.deliver(
                aNewMail()
                        .from("buyer13@buyer.com")
                        .to("seller13@seller.com")
                        .adId("232323233")
                        .plainBody("I would like to offer $10.")
                        .header(Header.OfferId.getValue(),"GTAU")
        );

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("I would like to offer $10."))
                .body("body.conversations[0].offerId", equalTo("GTAU"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller13@seller.com");
    }

    @Test
    public void conversationShouldNotHaveOfferIdWhenTheLastMessageIsNotOffer() throws MessagingException {
        testRule.deliver(
                aNewMail()
                        .from("buyer14@buyer.com")
                        .to("seller14@seller.com")
                        .adId("2323232334")
                        .plainBody("I would like to offer $10.")
                        .header(Header.OfferId.getValue(),"GTAU")
        );
        String anonymizedBuyer = testRule.waitForMail().getFrom()[0].toString();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("I would like to offer $10."))
                .body("body.conversations[0].offerId", equalTo("GTAU"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller14@seller.com");

        testRule.deliver(
                aNewMail()
                        .from("seller14@buyer.com")
                        .to(anonymizedBuyer)
                        .adId("2323232334")
                        .plainBody("Reply to the enquiry.")
        );
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(false))
                .body("body.conversations[0].textShortTrimmed", equalTo("Reply to the enquiry."))
                .body("body.conversations[0].offerId", equalTo(null))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller14@seller.com");
    }
}
