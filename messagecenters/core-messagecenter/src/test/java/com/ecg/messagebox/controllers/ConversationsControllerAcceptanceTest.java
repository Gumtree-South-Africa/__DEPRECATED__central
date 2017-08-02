package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.requests.EmptyConversationRequest;
import com.ecg.messagebox.util.EmptyConversationFixture;
import com.ecg.replyts.integration.test.MailBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.ExtractableResponse;
import com.jayway.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static com.ecg.messagebox.util.EmptyConversationFixture.*;

public class ConversationsControllerAcceptanceTest extends ReplyTsIntegrationTestRuleHelper {

    private static MailBuilder MAIL1 = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller2@seller.com")
            .header("X-CUST-" + "user-id-buyer", "1")
            .header("X-CUST-" + "user-id-seller", "2")
            .adId("232323")
            .plainBody("first contact from buyer 1");

    private static MailBuilder MAIL2 = aNewMail()
            .from("buyer3@buyer.com")
            .to("seller2@seller.com")
            .header("X-CUST-" + "user-id-buyer", "3")
            .header("X-CUST-" + "user-id-seller", "2")
            .adId("232323")
            .plainBody("first contact from buyer 3");

    @Test
    public void getConversations() {
        String convId1 = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();
        String convId2 = testRule.deliver(MAIL2).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(2))
                .body("body.unreadMessagesCount", equalTo(2))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(2))
                .body("body.conversations[0].id", equalTo(convId2))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].latestMessage.text", equalTo("first contact from buyer 3"))
                .body("body.conversations[1].id", equalTo(convId1))
                .body("body.conversations[1].adId", equalTo("232323"))
                .body("body.conversations[1].latestMessage.text", equalTo("first contact from buyer 1"))
                .body("body.offset", equalTo(0))
                .body("body.limit", equalTo(300))
                .body("body.totalCount", equalTo(2))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations?offset=0&limit=300");
    }

    @Test
    public void changeVisibilities() {
        String convId1 = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();
        String convId2 = testRule.deliver(MAIL2).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations[0].id", equalTo(convId2))
                .body("body.conversations[1].id", equalTo(convId1))
                .body("body.conversations[0].visibility", equalTo("active"))
                .body("body.conversations[1].visibility", equalTo("active"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].id", equalTo(convId2))
                .body("body.conversations[0].visibility", equalTo("active"))
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations?action=change-visibility&visibility=archived&ids=" + convId1);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].id", equalTo(convId1))
                .body("body.conversations[0].visibility", equalTo("archived"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations?visibility=archived");
    }

    @Test
    public void createValidEmptyConversationAndThenSendCannedEnquiry() throws Exception {

        /**
         * 1. Create an empty conversation
         */

        Response emptyConversationResponse = RestAssured
            .given()
                .body(toJson(validEmptyConversation()))
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .log().all()
            .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+BUYER_ID_1+"/ads/"+ADVERT_ID);

        ExtractableResponse emptyExtractableResponse = emptyConversationResponse
                                                    .then()
                                                    .statusCode(200)
                                                    .body("body", notNullValue())
                                                    .extract();

        String newConversationId = emptyExtractableResponse.path("body");

        /**
         * 2. Get conversation
         */

        RestAssured
            .given()
                .log().all()
            .when()
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+BUYER_ID_1+"/conversations/"+newConversationId)
            .then()
                .statusCode(200)
                .body("body.messages.size()", equalTo(0))
                .body("body.latestMessage.text", equalTo(AD_TITLE))
                .body("body.latestMessage.senderUserId", equalTo(BUYER_ID_1))
                .body("body.emailSubject", equalTo(AD_TITLE))
                .body("body.id", equalTo(newConversationId));


        /**
         * 3. Send canned enquiry
         */

        final String SELECTED_CANNED_ENQUIRY = "This is my canned enquiry";

        MailBuilder firstMessageForEmptyConversation = aNewMail()
                .from(BUYER_EMAIL_1)
                .to(SELLER_EMAIL_1)
                .header("X-CUST-" + "user-id-buyer", BUYER_ID_1)
                .header("X-CUST-" + "user-id-seller", SELLER_ID_1)
                .adId(ADVERT_ID)
                .plainBody(SELECTED_CANNED_ENQUIRY);

        String conversationIdAfterMail = testRule.deliver(firstMessageForEmptyConversation).getConversation().getId();
        testRule.waitForMail();

        Assert.assertEquals("conversation id should match", newConversationId, conversationIdAfterMail);

        /**
         * 4. Get conversation with canned enquiry
         */

        RestAssured
            .given()
                .log().all()
            .when()
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+BUYER_ID_1+"/conversations/"+newConversationId)
            .then()
                .statusCode(200)
                .body("body.messages.size()", equalTo(1))
                .body("body.messages[0].text", equalTo(SELECTED_CANNED_ENQUIRY))
                .body("body.latestMessage.text", equalTo(SELECTED_CANNED_ENQUIRY))
                .body("body.latestMessage.senderUserId", equalTo(BUYER_ID_1))
                .body("body.emailSubject", equalTo(AD_TITLE))
                .body("body.id", equalTo(newConversationId));
    }

    @Test
    public void createInvalidEmptyConversation() throws Exception {

        RestAssured.given()
                .body(toJson(invalidEmptyConversation()))
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/ads/123")
                .then()
                .statusCode(400);
    }

    @Test
    public void createEmptyConversationMissingParticipants() throws Exception {

        EmptyConversationRequest emptyConversationRequest = EmptyConversationFixture.validEmptyConversation();
        emptyConversationRequest.setParticipants(new HashMap());

        Response emptyConversationResponse = RestAssured
                .given()
                .body(toJson(emptyConversationRequest))
                .header("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .log().all()
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/"+BUYER_ID_1+"/ads/"+ADVERT_ID);

        emptyConversationResponse.then().statusCode(400);
    }

    private String toJson(Object request) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(request);
    }
}