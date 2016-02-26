package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxControllerUseUserIdAcceptanceTest {

    @BeforeClass
    public static void setup() throws Exception {
        System.setProperty("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        System.setProperty("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());
    }

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule("/mb-integration-test-conf", "/cassandra_schema.cql", "/cassandra_messagebox_schema.cql");

    @Test
    public void readConversation() {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].userId", equalTo("2"))
                .body("body.conversations[0].email", equalTo("2"))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");
    }

    @Test
    public void readMessages() throws Exception {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail().from("seller1@seller.com").to(anonymizedBuyer).plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();


        String conversationId = testRule.deliver(aNewMail().from("buyer1@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer.")).getConversation().getId();
        testRule.waitForMail();

        // now we have a conversation with three messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(conversationId))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.messages[0].textShort", equalTo("First contact from buyer."))
                .body("body.messages[0].boundness", equalTo("INBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("OUTBOUND"))
                .body("body.messages[2].textShort", equalTo("re-reply for buyer."))
                .body("body.messages[2].boundness", equalTo("INBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + conversationId);
    }

    @Test
    public void deleteConversation() {
        String conversationId = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        ).getConversation().getId();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations?ids=" + conversationId);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");

    }

    @Test
    public void resetMessageCounter() {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("First contact from first buyer.")
        ).getConversation().getId();
        testRule.waitForMail();
        testRule.deliver(
                aNewMail()
                        .from("buyer2@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("424242")
                        .plainBody("First contact from second buyer.")
        ).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(2))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .put("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2");
    }

}
