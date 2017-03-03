package com.ecg.messagecenter.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.hamcrest.Matchers.equalTo;

public class ConversationThreadControllerAcceptanceTest {
    private final Properties testProperties = new Properties() {{
        put("persistence.strategy", "riak");
    }};

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties, null, 20, ES_ENABLED);


    @Test
    public void testGetPostBoxConversationByEmailAndConversationId() throws Exception {
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

        // now we have a conversation with two unread messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.numUnread", equalTo(2))
                .body("body.messages[0].textShort", equalTo("First contact from buyer."))
                .body("body.messages[0].boundness", equalTo("INBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("OUTBOUND"))
                .body("body.messages[2].textShort", equalTo("re-reply for buyer."))
                .body("body.messages[2].boundness", equalTo("INBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller3@seller.com/conversations/" + conversationId);


        // now one is read
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.numUnread", equalTo(1))
                .put("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller3@seller.com/conversations/" + conversationId);

        // still one unread
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.numUnread", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer3@buyer.com/conversations/" + conversationId);
        
    }


}
