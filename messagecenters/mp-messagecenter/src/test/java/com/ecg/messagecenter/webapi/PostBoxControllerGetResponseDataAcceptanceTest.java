package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.identifier.UserIdentifierType;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.hamcrest.Matchers.equalTo;

public class PostBoxControllerGetResponseDataAcceptanceTest {
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("messagebox.userid.userIdentifierStrategy", UserIdentifierType.BY_USER_ID.toString());
        properties.put("userIdentifierService", UserIdentifierType.BY_USER_ID.toString());

        return properties;
    }).get(), "/mb-integration-test-conf", "cassandra_schema.cql", "cassandra_messagebox_schema.cql");

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


        testRule.deliver(aNewMail().from("buyer1@buyer.com").to(anonymizedSeller).plainBody("re-reply for buyer."));
        testRule.waitForMail();

        // now we have a response data for seller with id 2
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.responseData.size()", equalTo(1))
                .body("body.responseData[0].userId", equalTo("2"))
                .body("body.responseData[0].responseSpeed", equalTo(0))
                .body("body.responseData[0].conversationType", equalTo("EMAIL"))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/2/response-data");

        // we do not have a response data for buyer with id 1
        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.responseData.size()", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/postboxes/1/response-data");

    }
}
