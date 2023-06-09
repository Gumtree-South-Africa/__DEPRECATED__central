package com.ecg.messagecenter.gtau.webapi;

import com.ecg.messagecenter.gtau.persistence.Header;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.MessagingException;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxOverviewControllerAcceptanceTest {
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties(),
            null, 20, new Class[]{PostBoxOverviewControllerAcceptanceTest.class},
            "cassandra_schema.cql", "cassandra_messagebox_schema.cql", "cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_GTAU);
        properties.put("replyts.tenant", TENANT_GTAU);
        properties.put("webapi.sync.au.enabled", "true");
        properties.put("push-mobile.host", "localhost");
        properties.put("messages.conversations.enrichment.on.read", "true");
        properties.put("persistence.cassandra.conversation.class", "com.ecg.messagecenter.gtau.persistence.ConversationThread");
        return properties;
    }

    @Test
    public void readConversation() {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        ).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].email", equalTo("seller1@seller.com"))
                .body("body.conversations[0].adId", equalTo("232323"))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(conversation.getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(conversation.getSellerSecret()))
                .body("body.conversations[0].status", equalTo(conversation.getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");
    }

    @Test
    public void deleteConversation() {
        String conversationId = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        ).getConversation().getId();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com/conversations?ids=" + conversationId);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.conversations.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");
    }

    @Test
    public void readConversationWithRobotMessages() throws MessagingException {
        ProcessedMail processedMail = testRule.deliver(
                aNewMail()
                        .from("buyer11@buyer.com")
                        .to("seller11@seller.com")
                        .adId("23232323")
                        .plainBody("First contact from buyer.")
        );
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .body("body.conversations[0].buyerId", equalTo("buyer11@buyer.com"))
                .body("body.conversations[0].sellerId", equalTo("seller11@seller.com")).log().body()
                .body("body.conversations[0].buyerAnonymousEmail", equalTo(processedMail.getOutboundMail().getFrom()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(processedMail.getConversation().getSellerSecret()))
                .body("body.conversations[0].status", equalTo(processedMail.getConversation().getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com");

        ProcessedMail robotMail = testRule.deliver(
                aNewMail()
                        .from("seller11@buyer.com")
                        .to(processedMail.getOutboundMail().getFrom())
                        .adId("23232323")
                        .header(Header.Robot.getValue(), "GTAU")
                        .plainBody("A message from Gumtree Robot.")
        );
        testRule.waitForMail();

        // Seller View
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("First contact from buyer."))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(robotMail.getConversation().getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(robotMail.getConversation().getSellerSecret()))
                .body("body.conversations[0].status", equalTo(robotMail.getConversation().getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller11@seller.com");

        // Buyer View
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("A message from Gumtree Robot."))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(robotMail.getConversation().getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(robotMail.getConversation().getSellerSecret()))
                .body("body.conversations[0].status", equalTo(robotMail.getConversation().getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/buyer11@buyer.com");
    }

    @Test
    public void conversationShouldHaveOfferIdWhenTheLastMessageIsOffer() throws MessagingException {
        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("buyer13@buyer.com")
                        .to("seller13@seller.com")
                        .adId("232323233")
                        .plainBody("I would like to offer $10.")
                        .header(Header.OfferId.getValue(), "GTAU")
        ).getConversation();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("I would like to offer $10."))
                .body("body.conversations[0].offerId", equalTo("GTAU"))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(conversation.getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(conversation.getSellerSecret()))
                .body("body.conversations[0].status", equalTo(conversation.getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller13@seller.com");
    }

    @Test
    @Ignore("Was working in Riak mode, but not in Cassandra mode, needs a proper look")
    public void conversationShouldNotHaveOfferIdWhenTheLastMessageIsNotOffer() throws MessagingException {
        ProcessedMail mail = testRule.deliver(
                aNewMail()
                        .from("buyer14@buyer.com")
                        .to("seller14@seller.com")
                        .adId("2323232334")
                        .plainBody("I would like to offer $10.")
                        .header(Header.OfferId.getValue(), "GTAU")
        );
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(1))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(true))
                .body("body.conversations[0].textShortTrimmed", equalTo("I would like to offer $10."))
                .body("body.conversations[0].offerId", equalTo("GTAU"))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(mail.getConversation().getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(mail.getConversation().getSellerSecret()))
                .body("body.conversations[0].status", equalTo(mail.getConversation().getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller14@seller.com").prettyPeek();

        Conversation conversation = testRule.deliver(
                aNewMail()
                        .from("seller14@buyer.com")
                        .to(mail.getOutboundMail().getFrom())
                        .adId("2323232334")
                        .plainBody("Reply to the enquiry.")
        ).getConversation();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnread", equalTo(0))
                .body("body.conversations.size()", equalTo(1))
                .body("body.conversations[0].unread", equalTo(false))
                .body("body.conversations[0].textShortTrimmed", equalTo("Reply to the enquiry."))
                .body("body.conversations[0].offerId", equalTo(null))
                .body("body.conversations[0].buyerAnonymousEmail", containsString(conversation.getBuyerSecret()))
                .body("body.conversations[0].sellerAnonymousEmail", containsString(conversation.getSellerSecret()))
                .body("body.conversations[0].status", equalTo(conversation.getState().name()))
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller14@seller.com");
    }
}
