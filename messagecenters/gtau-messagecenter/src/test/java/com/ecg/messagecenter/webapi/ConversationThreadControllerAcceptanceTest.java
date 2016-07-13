package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.Header;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created by maotero on 6/10/2015.
 */
public class ConversationThreadControllerAcceptanceTest {
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

    @Test
    public void readMessages() throws Exception {
        testRule.deliver(
                aNewMail()
                        .from("buyer3@buyer.com")
                        .to("seller3@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail().from("seller3@seller.com").to(anonymizedBuyer).plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();


        String conversationId = testRule.deliver(aNewMail().from("buyer3@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer.")).getConversation().getId();
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
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller3@seller.com/conversations/" + conversationId);
    }

    @Test
    public void testRobotEnabledFlag() throws Exception {
        testRule.deliver(aNewMail()
                        .from("buyer4@buyer.com")
                        .to("seller4@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail()
                .from("seller4@seller.com")
                .to(anonymizedBuyer)
                .plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();

        testRule.deliver(aNewMail()
                        .from("seller4@buyer.com")
                        .to(anonymizedBuyer)
                        .adId("232323")
                        .header(Header.Robot.getValue(), "GTAU")
                        .plainBody("A message from Gumtree Robot.")
        );
        testRule.waitForMail();

        String conversationId = testRule.deliver(aNewMail()
                .from("buyer4@buyer.com")
                .to(anonymizedSeller)
                .plainBody("re-reply for buyer.")).getConversation().getId();
        testRule.waitForMail();

        // now we have a conversation with four messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(4))
                .body("body.messages[0].textShort", equalTo("First contact from buyer."))
                .body("body.messages[0].boundness", equalTo("OUTBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("INBOUND"))
                .body("body.messages[2].textShort", equalTo("A message from Gumtree Robot."))
                .body("body.messages[2].boundness", equalTo("INBOUND"))
                .body("body.messages[3].textShort", equalTo("re-reply for buyer."))
                .body("body.messages[3].boundness", equalTo("OUTBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer4@buyer.com/conversations/" + conversationId);

        // Exclude Robot Messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.messages[0].textShort", equalTo("First contact from buyer."))
                .body("body.messages[0].boundness", equalTo("OUTBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("INBOUND"))
                //.body("body.messages[2].textShort", equalTo("A message from Gumtree Robot."))
                //.body("body.messages[2].boundness", equalTo("INBOUND"))
                .body("body.messages[2].textShort", equalTo("re-reply for buyer."))
                .body("body.messages[2].boundness", equalTo("OUTBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer4@buyer.com/conversations/" + conversationId + "?robotEnabled=false");
    }
}
