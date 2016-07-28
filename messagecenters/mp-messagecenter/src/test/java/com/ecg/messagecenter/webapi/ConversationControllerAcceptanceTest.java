package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class ConversationControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        properties.put("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());

        return properties;
    }).get(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagebox_schema.cql", "cassandra_new_messagebox_schema.cql");

    private static final MailBuilder MAIL1 = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller1@seller.com")
            .header("X-CUST-" + "user-id-buyer", "1")
            .header("X-CUST-" + "user-id-seller", "2")
            .adId("232323")
            .plainBody("first contact from buyer");

    @Test
    public void getConversation() throws Exception {
        testRule.deliver(MAIL1);
        MimeMessage contactMail = testRule.waitForMail();
        String anonymizedBuyer = contactMail.getFrom()[0].toString();

        testRule.deliver(aNewMail().from("seller1@seller.com").to(anonymizedBuyer).plainBody("reply by seller"));
        MimeMessage replyMail = testRule.waitForMail();
        String anonymizedSeller = replyMail.getFrom()[0].toString();

        String convId = testRule.deliver(aNewMail().from("buyer1@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer"))
                .getConversation().getId();
        testRule.waitForMail();

        // now we have a conversation with three messages
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.numUnread", equalTo(2))
                .body("body.adId", equalTo("232323"))
                .body("body.messages.size()", equalTo(3))
                .body("body.messages[0].textShort", equalTo("first contact from buyer"))
                .body("body.messages[0].boundness", equalTo("INBOUND"))
                .body("body.messages[1].textShort", equalTo("reply by seller"))
                .body("body.messages[1].boundness", equalTo("OUTBOUND"))
                .body("body.messages[2].textShort", equalTo("re-reply for buyer"))
                .body("body.messages[2].boundness", equalTo("INBOUND"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + convId);
    }

    @Test
    public void markConversationAsRead() throws Exception {
        String convId = testRule.deliver(MAIL1).getConversation().getId();
        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.numUnread", equalTo(1))
                .body("body.messages.size()", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + convId);

        // mark conversation as read
        RestAssured.given()
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + convId);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.id", equalTo(convId))
                .body("body.numUnread", equalTo(0))
                .body("body.messages.size()", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + convId);
    }
}