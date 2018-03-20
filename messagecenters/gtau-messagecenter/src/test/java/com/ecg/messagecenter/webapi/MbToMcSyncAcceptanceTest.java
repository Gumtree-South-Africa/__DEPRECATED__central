package com.ecg.messagecenter.webapi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.junit.Test;

import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.AD_ID;
import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.CUSTOM_FROM_USER_ID;
import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.CUSTOM_TO_USER_ID;
import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.FROM;
import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.MESSAGE;
import static com.ecg.messagecenter.webapi.SyncTestBase.TestValues.TO;
import static org.hamcrest.Matchers.equalTo;

public class MbToMcSyncAcceptanceTest extends SyncTestBase {

    @Test
    public void bothMcAndMbShouldHaveSameMessage() {
        String conversationId = testRule.deliver(buildMail()).getConversation().getId();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo(AD_ID.value))
                .body("body.buyerEmail", equalTo(FROM.value))
                .body("body.sellerEmail", equalTo(TO.value))
                .body("body.messages[0].textShort", equalTo(MESSAGE.value))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations/" + conversationId)
                .prettyPeek();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("id", equalTo(conversationId))
                .body("adId", equalTo(AD_ID.value))
                .body("participants[0].userId", equalTo(CUSTOM_FROM_USER_ID.value))
                .body("participants[0].email", equalTo(FROM.value))
                .body("participants[1].userId", equalTo(CUSTOM_TO_USER_ID.value))
                .body("participants[1].email", equalTo(TO.value))
                .body("latestMessage.text", equalTo(MESSAGE.value))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/" + conversationId)
                .prettyPeek();
    }

    @Test
    public void markConversationAsReadShouldBeInSync() {
        String conversationId = testRule.deliver(buildMail()).getConversation().getId();

        testRule.waitForMail();

        RestAssured.put("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/" + conversationId + "/read");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations/" + conversationId)
                .prettyPeek();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("unreadMessagesCount", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/" + conversationId)
                .prettyPeek();
    }

    @Test
    public void deletingConversationShouldUpdateBothMcAndMb() {
        String conversationId = testRule.deliver(buildMail()).getConversation().getId();

        testRule.waitForMail();

        // Checking that conversation exists in both MC and MB
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations/" + conversationId)
                .prettyPeek();
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("visibility", equalTo("active"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/" + conversationId)
                .prettyPeek();

        // Archiving conversation through MB API
        RestAssured.given()
                .body("[\"" + conversationId + "\"]")
                .contentType(ContentType.JSON)
                .put("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/archive")
                .prettyPeek();

        // Checking that conversation removed in both MC and MB
        RestAssured.given()
                .expect()
                .statusCode(404)
                .body("body", equalTo("ENTITY_NOT_FOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations/" + conversationId)
                .prettyPeek();
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("visibility", equalTo("archived"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/" + CUSTOM_FROM_USER_ID.value + "/conversations/" + conversationId)
                .prettyPeek();
    }
}
