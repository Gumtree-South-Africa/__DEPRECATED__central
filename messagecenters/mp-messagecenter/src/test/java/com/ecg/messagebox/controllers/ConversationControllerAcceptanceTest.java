package com.ecg.messagebox.controllers;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class ConversationControllerAcceptanceTest extends ReplyTsIntegrationTestRuleHelper {

    private static MailBuilder MAIL1 = aNewMail()
            .from("buyer@buyer.com")
            .to("seller@seller.com")
            .header("X-CUST-" + "user-id-buyer", "1")
            .header("X-CUST-" + "user-id-seller", "2")
            .header("X-Message-Metadata", "message metadata")
            .adId("232323")
            .plainBody("first contact from buyer");

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForNewModel();

    @Test
    public void getConversation() throws Exception {
        testRule.deliver(MAIL1);
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail().from("seller@seller.com").to(anonymizedBuyer).plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();

        String convId = testRule.deliver(aNewMail().from("buyer@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer"))
                .getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.adId", equalTo("232323"))
                .body("body.visibility", equalTo("active"))
                .body("body.messageNotification", equalTo("receive"))
                .body("body.unreadMessagesCount", equalTo(2))
                .body("body.messages.size()", equalTo(3))
                .body("body.messages[0].text", equalTo("first contact from buyer"))
                .body("body.messages[0].customData", equalTo("message metadata"))
                .body("body.messages[1].text", equalTo("reply by seller"))
                .body("body.messages[2].text", equalTo("re-reply for buyer"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId);
    }

    @Test
    public void markConversationAsRead() throws Exception {
        String convId = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.unreadMessagesCount", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId);

        // mark conversation as read
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.unreadMessagesCount", equalTo(0))
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId + "?action=mark-as-read");
    }
}