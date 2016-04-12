package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener.ProcessedMail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.ecg.replyts.integration.test.filter.SubjectKeywordFilterFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static com.ecg.replyts.integration.test.support.Waiter.await;
import static org.hamcrest.Matchers.is;

public class MessageModerationAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);
    private ProcessedMail processedMail;

    private static int counter = 1;

    @Before
    public void setUp() {
        rule.registerConfig(SubjectKeywordFilterFactory.class, JsonObjects.builder().attr("foo", "bar").build());

        processedMail = rule.deliver(MailBuilder.aNewMail().from("a"+(++counter)+"@b.com").to("b@c.com").adId("23222").subject("HELD").plainBody("foobar"));
        Assert.assertEquals(MessageState.HELD, processedMail.getMessage().getState());
    }

    @Test
    public void isAbleToSendMailWhenModeratedAsGood() throws Exception {
        String convId = processedMail.getConversation().getId();
        String msgId = processedMail.getMessage().getId();

        RestAssured
                .expect()
                .statusCode(200)
                .and()
                .body("status.state", CoreMatchers.is("SUCCESS"))
                .when()
                .request()
                .body("{\"newMessageState\":\"GOOD\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/message/" + convId + "/" + msgId + "/state");

        Assert.assertNotNull(rule.waitForMail());
    }


    @Test
    public void noSentMailIfMessageMarkedBad() throws Exception {

        String convId = processedMail.getConversation().getId();
        String msgId = processedMail.getMessage().getId();

        RestAssured
                .expect()
                .statusCode(200)
                .and()
                .body("status.state", CoreMatchers.is("SUCCESS"))
                .when()
                .request()
                .body("{\"newMessageState\":\"BAD\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/message/" + convId + "/" + msgId + "/state");

        rule.assertNoMailArrives();
    }

    @Test
    public void moderatesMailToBlockedAllowsToSearchForModeratingCsAgent() {
        final String agentName = "agent-" + System.currentTimeMillis();

        String convId = processedMail.getConversation().getId();
        String msgId = processedMail.getMessage().getId();


        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .body("{\"newMessageState\":\"GOOD\", \"editor\":\"" + agentName + "\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/message/" + convId + "/" + msgId + "/state");


        await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String payload = String.format("{\"lastEditor\": \"%s\"}", agentName);
                try {

                    RestAssured
                            .expect()
                            .that()
                            .statusCode(HttpStatus.SC_OK)
                            .and()
                            .body("body.size()", is(1))
                            .when()
                            .request()
                            .contentType(ContentType.JSON)
                            .body(payload)
                            .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/message/search")
                            .andReturn();

                } catch (AssertionError e) {
                    // try until the assertion is valid
                    return false;
                }
                return true;
            }
        }).within(10, TimeUnit.SECONDS);
    }

    @Test
    public void mailToSentAfterModerationToGood() throws Exception {

        String convId = processedMail.getConversation().getId();
        String msgId = processedMail.getMessage().getId();

        RestAssured
                .expect()
                .statusCode(200)
                .and()
                .body("status.state", CoreMatchers.is("SUCCESS"))
                .when()
                .request()
                .body("{\"newMessageState\":\"GOOD\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/message/" + convId + "/" + msgId + "/state");

        Assert.assertNotNull(rule.waitForMail());

        RestAssured.expect()
                .body("body.messages[0].state", CoreMatchers.is("SENT"))
                .when()
                .request()
                .get("http://localhost:" + rule.getHttpPort() + "/screeningv2/conversation/" + convId);

    }

}
