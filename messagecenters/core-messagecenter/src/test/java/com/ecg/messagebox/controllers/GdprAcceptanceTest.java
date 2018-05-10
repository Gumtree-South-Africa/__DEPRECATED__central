package com.ecg.messagebox.controllers;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GdprAcceptanceTest extends ReplyTsIntegrationTestRuleHelper {

    @Test
    public void conversationCanBeClosedCorrectly() throws Exception {
        // setup a conversation
        String convId = testRule.deliver(
                aNewMail()
                        .from("sam@example.com")
                        .to("max@example.com")
                        .header("X-CUST-user-id-buyer", "25")
                        .header("X-CUST-user-id-seller", "30")
                        .adId("1234")
                        .plainBody("Hello max, I want to buy your stuff")
        ).getConversation().getId();

        String buyerEmail = testRule.waitForMail().getFrom()[0].toString();

        assertThat(messagesInMessageboxForUser("25"), is(1));
        assertThat(messagesInMessageboxForUser("30"), is(1));

        // close the conversation
        // curl -H "Content-Type: application/json" -X PUT -d "{ 'state':'CLOSED', 'issuerEmail':'Geert2'}" -vvv "http://localhost:18081/screeningv2/conversation/1:1w35b82:29qj12jhn"
        RestAssured
                .given()
                .body("{ 'state':'CLOSED', 'issuerEmail':'CS-Agent', 'userId':'25'}")
                .contentType("application/json")
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/screeningv2/conversation/" + convId);

        // assert that messagebox is empty for DDR initiating user
        assertThat(messagesInMessageboxForUser("25"), is(0));

        // assert that other party is not affected
        assertThat(messagesInMessageboxForUser("30"), is(1));

        // send a new message by user 30
        testRule.deliver(
                aNewMail()
                        .from("max@example.com")
                        .to(buyerEmail)
                        .adId("1234")
                        .plainBody("Hi sam, are you still there?"));

        // no new message should be in the messageboxes
        assertThat(messagesInMessageboxForUser("25"), is(0));
        assertThat(messagesInMessageboxForUser("30"), is(1));
    }

    private int messagesInMessageboxForUser(String userId) {
        String response = RestAssured.given()
                .expect()
                .statusCode(200)
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/" + userId + "/conversations")
                .asString();

        return JsonPath.from(response).getInt("body.conversations.size");
    }
}
