package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;

public class ConversationThreadControllerIntegrationTest {

    public static final MailBuilder MAIL = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller1@seller.com")
            .adId("232323")
            .plainBody("First contact from buyer.");

    private final Properties testProperties = new Properties() {{
        put("persistence.strategy", "riak");
    }};

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties, null, 20, ES_ENABLED);

    @Test
    public void existingConversation_sellerBlocksUnblocks() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

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
                .statusCode(HttpStatus.NO_CONTENT.value())
                .delete("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

    }

    @Test
    public void unknownConversation_blocksNotFound() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId() + "WTB"
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .delete("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId() + "WTB"
                        + "/block");
    }

    @Test
    public void unknownEmailInConversation_blocksNotFound() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

        testRule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/not-part-of-convo@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .delete("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/not-part-of-convo@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");
    }

    @Test
    public void sellerBlocksAndUnblocksConversation_statusReportedInFetch() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

        testRule.waitForMail();

        // Assert that nothing's blocked initially
        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body("status.state", Matchers.is("OK"))
                .body("body.blockedByBuyer", Matchers.is(false))
                .body("body.blockedBySeller", Matchers.is(false))
                .get("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId());

        // Seller blocks the buyer
        RestAssured.post("http://localhost:"
                + testRule.getHttpPort()
                + "/message-center/postboxes/seller1@seller.com/conversations/"
                + conversation.getId()
                + "/block");

        // Confirm that block is set + reported
        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body("status.state", Matchers.is("OK"))
                .body("body.blockedByBuyer", Matchers.is(false))
                .body("body.blockedBySeller", Matchers.is(true))
                .get("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId());

        // Seller unblocks the buyer
        RestAssured.delete("http://localhost:"
                + testRule.getHttpPort()
                + "/message-center/postboxes/seller1@seller.com/conversations/"
                + conversation.getId()
                + "/block");

        // Confirm that block is no longer set
        RestAssured.given()
                .expect()
                .statusCode(HttpStatus.OK.value())
                .body("status.state", Matchers.is("OK"))
                .body("body.blockedByBuyer", Matchers.is(false))
                .body("body.blockedBySeller", Matchers.is(false))
                .get("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId());
    }
}
