package com.ecg.messagebox.controllers;

import com.jayway.restassured.RestAssured;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class UnreadCountsControllerAcceptanceTest extends BaseControllerAcceptanceTest {

    @Test
    public void getUserUnreadCounts() {
        // create conversation 1
        String convId1 = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller2@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("first contact from buyer 1")
        ).getConversation().getId();
        testRule.waitForMail();

        // create conversation 2
        for (int i = 0; i < 3; i++) {
            testRule.deliver(
                    aNewMail()
                            .from("buyer3@buyer.com")
                            .to("seller2@seller.com")
                            .header("X-CUST-" + "user-id-buyer", "3")
                            .header("X-CUST-" + "user-id-seller", "2")
                            .adId("424242")
                            .plainBody("contact " + (i + 1) + " from buyer 3"));
            testRule.waitForMail();
        }

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.userId", equalTo("2"))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(2))
                .body("body.unreadMessagesCount", equalTo(4))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/unread-counts");

        // mark conversation 1 as read
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.unreadMessagesCount", equalTo(0))
                .post("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/conversations/" + convId1 + "?action=mark-as-read");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.userId", equalTo("2"))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(1))
                .body("body.unreadMessagesCount", equalTo(3))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/2/unread-counts");
    }
}