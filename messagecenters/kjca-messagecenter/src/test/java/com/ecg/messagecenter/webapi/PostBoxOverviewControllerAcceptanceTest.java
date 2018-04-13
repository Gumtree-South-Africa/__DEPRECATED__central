package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import javax.mail.internet.MimeMessage;

import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

public class PostBoxOverviewControllerAcceptanceTest {
    private final Properties testProperties = new Properties() {{
        put("replyts.tenant", "kjca");
        put("persistence.strategy", "riak");
        put("unread.count.cache.queue", "devull");
    }};

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties, null, 20, ES_ENABLED);

    @Test
    public void getConversationWithFilter() throws Exception {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from first buyer.")
        );
        testRule.waitForMail();
        testRule.deliver(
                aNewMail()
                        .from("buyer2@buyer.com")
                        .to("seller1@seller.com")
                        .adId("424242")
                        .plainBody("First contact from second buyer.")
        );
        testRule.waitForMail();

        testRule.deliver(
                aNewMail()
                        .from("seller1@seller.com")
                        .to("buyer1@buyer.com")
                        .adId("123456")
                        .plainBody("Contact from first seller to buyer about another ad.")
        );
        testRule.waitForMail();

        //without role filter
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body['_meta'].numFound", equalTo(3))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");

        //filter by role as a buyer, only 1 conversation should be available
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body['_meta'].numFound", equalTo(1))
                .body("body.conversations.adId", hasItem("123456"))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com?role=Buyer");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body['_meta'].numFound", equalTo(2))
                .body("body.conversations.adId", hasItems("232323", "424242"))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com?role=Seller");
    }

    @Test
    public void readConversation() {
        testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        );

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].email", equalTo("seller1@seller.com"))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }

    @Test
    public void sellerBlocksBuyer_statusReportedInOverview() throws Exception {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.ACCEPTED.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].blockedBySeller", equalTo(true))
                .body("body.conversations[0].blockedByBuyer", equalTo(false))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }

    @Test
    public void sellerRepliesToSelf_blocksSelf_statusReportedInOverview() throws Exception {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("seller1@seller.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.ACCEPTED.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].blockedBySeller", equalTo(true))
                .body("body.conversations[0].blockedByBuyer", equalTo(true))
                .get("http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com");
    }
}
