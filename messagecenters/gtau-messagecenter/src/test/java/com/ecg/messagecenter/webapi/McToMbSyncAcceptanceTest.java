package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.AD_ID;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.CUSTOM_FROM_USER_ID;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.CUSTOM_TO_USER_ID;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.FROM;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.MESSAGE;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.SUBJECT;
import static com.ecg.messagecenter.webapi.McToMbSyncAcceptanceTest.TestValues.TO;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.hamcrest.Matchers.equalTo;

public class McToMbSyncAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(
            new Properties() {{
                put("replyts.tenant", "gtau");
                put("persistence.strategy", "cassandra");

                put("webapi.sync.au.enabled", "true");
                put("webapi.diff.au.enabled", "true");
                put("messagebox.userid.by_user_id.customValueNameForBuyer", "from-userid");
                put("messagebox.userid.by_user_id.customValueNameForSeller", "to-userid");
                put("messagebox.userid.userIdentifierStrategy", "BY_USER_ID");
            }},
            null, 20, ES_ENABLED,
            new Class[]{ConversationThreadControllerAcceptanceTest.class},
            "cassandra_schema.cql", "cassandra_messagebox_schema.cql", "cassandra_messagecenter_schema.cql");

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

        RestAssured.put("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations/" + conversationId);

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

        // Removing conversation through MC API
        RestAssured.delete("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + FROM.value + "/conversations?ids=" + conversationId);

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

    private static MailBuilder buildMail() {
        return aNewMail()
                .from(FROM.value)
                .to(TO.value)
                .adId(AD_ID.value)
                .customHeader("from-userid", CUSTOM_FROM_USER_ID.value)
                .customHeader("to-userid", CUSTOM_TO_USER_ID.value)
                .subject(SUBJECT.value)
                .plainBody(MESSAGE.value);
    }

    enum TestValues {
        FROM("from.mail@example.com"),
        TO("to.mail@example.com"),
        AD_ID("12345"),
        SUBJECT("Random subject"),
        MESSAGE("Just a string with text"),
        CUSTOM_FROM_USER_ID("user1"),
        CUSTOM_TO_USER_ID("user2");

        private final String value;

        TestValues(String value) {
            this.value = value;
        }
    }
}
