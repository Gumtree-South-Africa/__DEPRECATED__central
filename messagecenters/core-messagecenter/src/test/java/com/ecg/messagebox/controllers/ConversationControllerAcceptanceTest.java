package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.requests.SystemMessagePayload;
import com.ecg.replyts.integration.test.MailBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import org.junit.Test;
import org.springframework.http.MediaType;

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

    private static MailBuilder FIRST_MAIL_FOR_SYSTEM_MSG = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller2@seller.com")
            .header("X-CUST-" + "user-id-buyer", "1")
            .header("X-CUST-" + "user-id-seller", "100")
            .adId("abc")
            .plainBody("not relevant message");


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
                .when()
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId)
                .then()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.unreadMessagesCount", equalTo(1));

        // mark conversation as read
        RestAssured.given()
                .when()
                .header("Content-Type", "application/json")
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId + "?action=mark-as-read")
                .then()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.unreadMessagesCount", equalTo(0));
    }

    @Test
    public void postSystemMessageWithoutPayload() {
        RestAssured.given()
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/abc/system-messages")
                .then()
                .statusCode(500);
    }

    @Test
    public void postSystemMessageWithInvalidPayload() throws Exception {
        SystemMessagePayload payload = new SystemMessagePayload();

        RestAssured.given()
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(toJson(payload))
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/abc/system-messages")
                .then()
                .statusCode(200)
                .body("body", equalTo("INVALID_ARGUMENTS"));
    }

    @Test
    public void postSystemMessageSuccessfully() throws Exception {
        String conversationId = testRule.deliver(FIRST_MAIL_FOR_SYSTEM_MSG).getConversation().getId();
        int sellerId = 100;
        testRule.waitForMail();

        SystemMessagePayload payload = new SystemMessagePayload();
        payload.setText("some text");
        payload.setAdId("abc");
        payload.setCustomData("some custom data");

        RestAssured.given()
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .body(toJson(payload))
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+ sellerId +"/conversations/" + conversationId + "/system-messages")
                .then()
                .statusCode(200)
                .body("body", equalTo("OK"));

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations[0].adId", equalTo("abc"))
                .body("body.conversations[0].latestMessage.type", equalTo("systemMessage"))
                .body("body.conversations[0].latestMessage.senderUserId", equalTo("-1"))
                .body("body.conversations[0].latestMessage.text", equalTo("some text"))
                .body("body.conversations[0].latestMessage.customData", equalTo("some custom data"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+ sellerId +"/conversations?offset=0&limit=300");
    }

    private static String toJson(Object request) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(request);
    }
}