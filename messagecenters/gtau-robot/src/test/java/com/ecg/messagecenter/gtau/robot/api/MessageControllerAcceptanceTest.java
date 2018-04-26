package com.ecg.messagecenter.gtau.robot.api;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;

public class MessageControllerAcceptanceTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(createProperties(), true);

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_GTAU);
        properties.put("rabbitmq.host", "localhost");
        properties.put("rabbitmq.username", "guest");
        properties.put("rabbitmq.password", "guest");
        properties.put("rabbitmq.virtualHost", "/");
        properties.put("rabbitmq.endpoint", "gtau");
        properties.put("rabbitmq.connectionTimeout", "1000");
        properties.put("rabbitmq.port", "5672");
        properties.put("mailpublisher.kafka.broker.list", "localhost:9092");
        return properties;
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

        Thread.sleep(1000);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .and()
                .body("body.adId", equalTo(adId))
                .body("body.conversationIds.size()", equalTo(0))
                .when()
                .request()
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
                .get("http://localhost:" + rule.getHttpPort() + format("/gtau-robot/users/%s/ads/%s", seller, adId));
    }
}
