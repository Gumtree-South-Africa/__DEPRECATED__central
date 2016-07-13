package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import javax.mail.internet.MimeMessage;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxOverviewControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

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
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }

    @Test
    public void readMessages() throws Exception {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail().from("seller1@seller.com").to(anonymizedBuyer).plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();


        String conversationId = testRule.deliver(aNewMail().from("buyer1@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer.")).getConversation().getId();
        testRule.waitForMail();

        // now we have a conversation with three messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.messages[0].textShort", equalTo("First contact from buyer."))
                .body("body.messages[0].boundness", equalTo("INBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("OUTBOUND"))
                .body("body.messages[2].textShort", equalTo("re-reply for buyer."))
                .body("body.messages[2].boundness", equalTo("INBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com/conversations/" + conversationId);
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
                .delete("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com/conversations?ids=" + conversationId);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");

    }

    @Test
    public void resetMessageCounter() {
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
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(2))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");

        // counter not reset in old counter mode
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(2))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");

        // counter reset in new counter mode
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com?newCounterMode=true");
    }

    @Test
    public void sellerBlocksBuyer_statusReportedInOverview() throws Exception {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.ACCEPTED.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].blockedBySeller", equalTo(true))
                .body("body.conversations[0].blockedByBuyer", equalTo(false))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }

    @Test
    public void sellerRepliesToSelf_blocksSelf_statusReportedInOverview() throws Exception {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("seller1@seller.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.ACCEPTED.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].blockedBySeller", equalTo(true))
                .body("body.conversations[0].blockedByBuyer", equalTo(true))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }
}
