package com.ecg.messagebox.controllers;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

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

    @Rule
    public ReplyTsIntegrationTestRule testRule = getTestRuleForNewModel();

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
}