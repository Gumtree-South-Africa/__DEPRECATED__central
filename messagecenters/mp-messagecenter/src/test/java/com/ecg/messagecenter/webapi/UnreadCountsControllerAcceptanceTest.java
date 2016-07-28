package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class UnreadCountsControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        properties.put("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());

        return properties;
    }).get(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagebox_schema.cql", "cassandra_new_messagebox_schema.cql");

    @Test
    public void getPostBoxUnreadCounts() {
        String convId1 = testRule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .header("X-CUST-" + "user-id-buyer", "1")
                        .header("X-CUST-" + "user-id-seller", "2")
                        .adId("232323")
                        .plainBody("first contact from first buyer")
        ).getConversation().getId();
        testRule.waitForMail();

        for (int i = 0; i < 3; i++) {
            testRule.deliver(
                    aNewMail()
                            .from("buyer2@buyer.com")
                            .to("seller1@seller.com")
                            .header("X-CUST-" + "user-id-buyer", "3")
                            .header("X-CUST-" + "user-id-seller", "2")
                            .adId("424242")
                            .plainBody("contact " + (i + 1) + " from second buyer"));
            testRule.waitForMail();
        }

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnreadConversations", equalTo(2))
                .body("body.numUnreadMessages", equalTo(4))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/unread-counters");

        // mark conversation 1 as read
        RestAssured.given()
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/conversations/" + convId1);

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.numUnreadConversations", equalTo(1))
                .body("body.numUnreadMessages", equalTo(3))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/unread-counters");
    }
}