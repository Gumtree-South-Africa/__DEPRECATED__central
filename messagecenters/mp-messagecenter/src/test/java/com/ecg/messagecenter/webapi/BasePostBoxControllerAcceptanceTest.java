package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;

class BasePostBoxControllerAcceptanceTest {

    private static MailBuilder MAIL1 = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller1@seller.com")
            .header("X-CUST-" + "user-id-buyer", "1")
            .header("X-CUST-" + "user-id-seller", "2")
            .adId("232323")
            .plainBody("first contact from buyer 1");

    private static MailBuilder MAIL2 = aNewMail()
            .from("buyer2@buyer.com")
            .to("seller1@seller.com")
            .header("X-CUST-" + "user-id-buyer", "3")
            .header("X-CUST-" + "user-id-seller", "2")
            .adId("232323")
            .plainBody("first contact from buyer 3");

    void getPostBox(ReplyTsIntegrationTestRule testRule, boolean newModelEnabled) {
        String convId1 = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();
        String convId2 = testRule.deliver(MAIL2).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(2))
                .body("body.numUnread", equalTo(2))
                .body("body.conversations[0].id", equalTo(convId2))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].textShortTrimmed", equalTo("first contact from buyer 3"))
                .body("body.conversations[0].creationDate", newModelEnabled ? equalTo(toFormattedTimeISO8601ExplicitTimezoneOffset(now())) : nullValue())
                .body("body.conversations[1].id", equalTo(convId1))
                .body("body.conversations[1].adId", equalTo("232323"))
                .body("body.conversations[1].textShortTrimmed", equalTo("first contact from buyer 1"))
                .body("body.conversations[1].creationDate", newModelEnabled ? equalTo(toFormattedTimeISO8601ExplicitTimezoneOffset(now())) : nullValue())
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");
    }

    void deleteConversations(ReplyTsIntegrationTestRule testRule) {
        String convId1 = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();
        String convId2 = testRule.deliver(MAIL2).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(2))
                .body("body.conversations[0].id", equalTo(convId2))
                .body("body.conversations[1].id", equalTo(convId1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");

        // delete conversations
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .delete("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations?ids=" + convId1 + "," + convId2);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");
    }
}
