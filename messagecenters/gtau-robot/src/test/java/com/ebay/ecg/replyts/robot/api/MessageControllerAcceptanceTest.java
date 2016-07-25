package com.ebay.ecg.replyts.robot.api;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Profile;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;

@Profile(ReplyTS.EMBEDDED_PROFILE)
public class MessageControllerAcceptanceTest {

    private final Properties properties = new Properties() {{
        put("persistence.strategy", "riak");
    }};

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(properties, null, 20, ES_ENABLED);

    private static RabbitMQRunner rmq = new RabbitMQRunner();

    @BeforeClass
    public static void setup() {
        rmq.start();
        System.setProperty("rabbitmq.host", "localhost");
        System.setProperty("rabbitmq.username", "guest");
        System.setProperty("rabbitmq.password", "guest");
        System.setProperty("rabbitmq.virtualHost", "/");
        System.setProperty("rabbitmq.endpoint", "gtau");
        System.setProperty("rabbitmq.connectionTimeout", "1000");
        System.setProperty("rabbitmq.port", "5672");
    }

    @Test
    public void postMessageToConversation() throws Exception {
        String seller = "seller1@seller.com";
        String buyer = "buyer1@seller.com";
        String ad = "11111";

        Conversation conversation = rule.deliver(
                aNewMail()
                        .from(buyer)
                        .to(seller)
                        .adId(ad)
                        .plainBody("First contact from buyer.")
        ).getConversation();

        rule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .when()
                .request()
                .body("{\"message\":\"Hello World!\",\"messageDirection\":\"SELLER_TO_BUYER\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + "/gtau-robot/conversations/" + conversation.getId());



        rule.waitForMail();
    }

    @Test
    public void postMessageToConversationsForAd() throws Exception {
        String seller = "seller2@seller.com";
        String buyer1 = "buyer21@buyer.com";
        String buyer2 = "buyer22@buyer.com";
        String ad = "11112";

        rule.deliver(
                aNewMail()
                        .from(buyer1)
                        .to(seller)
                        .adId(ad)
                        .plainBody("First contact from buyer1.")
        );

        rule.waitForMail();

        rule.deliver(
                aNewMail()
                        .from(buyer2)
                        .to(seller)
                        .adId(ad)
                        .plainBody("First contact from buyer2.")
        );

        rule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .and()
                .body("body.status", equalTo("OK"))
                .when()
                .request()
                .body("{\"message\":\"Hi there!\",\"messageDirection\":\"SELLER_TO_BUYER\"}")
                .header("Content-Type", "application/json")
                .post("http://localhost:" + rule.getHttpPort() + format("/gtau-robot/users/%s/ads/%s",seller,ad));
    }

    @Test
    public void getConversationsWithNoReply() throws Exception {
        String seller = "seller3@seller.com";
        String buyer1 = "buyer31@seller.com";
        String buyer2 = "buyer32@seller.com";
        String adId = "11113";

        rule.deliver(
                aNewMail()
                        .from(buyer1)
                        .to(seller)
                        .adId(adId)
                        .plainBody("First contact from buyer1.")
        ).getConversation().getId();
        MimeMessage contactMail = rule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        rule.deliver(
                aNewMail()
                        .from(seller)
                        .to(anonymizedBuyer)
                        .adId(adId)
                        .plainBody("Reply from seller1.")
        );
        rule.waitForMail();

        String conv2 = rule.deliver(
                aNewMail()
                        .from(buyer2)
                        .to(seller)
                        .adId(adId)
                        .plainBody("First contact from buyer2.")
        ).getConversation().getId();
        rule.waitForMail();

        Thread.sleep(1000);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .and()
                .body("body.adId", equalTo(adId))
                .body("body.conversationIds.size()", equalTo(1))
                .body("body.conversationIds[0]", equalTo(conv2))
                .when()
                .request()
                .header("Content-Type", "application/json")
                .get("http://localhost:" + rule.getHttpPort() + format("/gtau-robot/users/%s/ads/%s", seller, adId));
    }

    @Test
    public void testWhenNoConversationsReturned() throws Exception {
        String seller = "seller41@seller.com";
        String buyer = "buyer41@seller.com";
        String adId = "11114";

        rule.waitUntilIndexedInEs(rule.deliver(
                aNewMail()
                        .from(buyer)
                        .to(seller)
                        .adId(adId)
                        .plainBody("First contact from buyer2.")
        ));
        MimeMessage contactMail = rule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        rule.waitUntilIndexedInEs(rule.deliver(
                aNewMail()
                        .from(seller)
                        .to(anonymizedBuyer)
                        .adId(adId)
                        .plainBody("Reply from seller2.")
        ));
        rule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .and()
                .body("body.adId", equalTo(adId))
                .body("body.conversationIds.size()", equalTo(0))
                .when()
                .request()
                .header("Content-Type", "application/json")
                .get("http://localhost:" + rule.getHttpPort() + format("/gtau-robot/users/%s/ads/%s", seller, adId));
    }

    @Test
    public void testWhenSingleConversationReturned() throws Exception {
        String seller = "seller5@seller.com";
        String buyer = "buyer51@seller.com";
        String adId = "11115";

        String conv = rule.deliver(
                aNewMail()
                        .from(buyer)
                        .to(seller)
                        .adId(adId)
                        .plainBody("First contact from buyer3.")
        ).getConversation().getId();
        rule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .and()
                .body("body.adId", equalTo(adId))
                .body("body.conversationIds.size()", equalTo(1))
                .body("body.conversationIds[0]", equalTo(conv))
                .when()
                .request()
                .header("Content-Type", "application/json")
                .get("http://localhost:" + rule.getHttpPort() + format("/gtau-robot/users/%s/ads/%s", seller, adId));
    }
}
