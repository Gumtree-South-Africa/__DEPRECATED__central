package com.ecg.messagecenter.kjca.webapi;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

public class ConversationThreadControllerIntegrationTest {
    private static final MailBuilder MAIL = aNewMail()
            .from("buyer1@buyer.com")
            .to("seller1@seller.com")
            .adId("232323")
            .plainBody("First contact from buyer.");

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties())
            .addCassandraSchema("cassandra_kjca_messagecenter_schema.cql")
            .addCassandraSchema("cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_KJCA);
        properties.put("unread.count.cache.queue", "devull");
        properties.put("persistence.cassandra.conversation.class", "com.ecg.messagecenter.kjca.persistence.ConversationThread");
        return properties;
    }
    @Test
    public void deleteConversation_removedFromPostbox() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

        testRule.waitForMail();

        String postboxEndpoint = "http://localhost:" + testRule.getHttpPort() + "/message-center/postboxes/seller1@seller.com";
        String singleConvoURI = postboxEndpoint + "/conversations/" + conversation.getId();
        RestAssured.expect()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .delete(singleConvoURI);

        RestAssured.expect()
                .statusCode(HttpStatus.OK.value())
                .body("status.state", Matchers.is("OK"))
                .body("body.conversations", Matchers.empty())
                .get(postboxEndpoint);
    }


    @Test
    public void existingConversation_sellerBlocksUnblocks() throws Exception {
        Conversation conversation = testRule.deliver(MAIL).getConversation();

        testRule.waitForMail();

        RestAssured.expect()
                .statusCode(HttpStatus.ACCEPTED.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.expect()
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

        RestAssured.expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/seller1@seller.com/conversations/"
                        + conversation.getId() + "WTB"
                        + "/block");

        RestAssured.expect()
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

        RestAssured.expect()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .post("http://localhost:"
                        + testRule.getHttpPort()
                        + "/message-center/postboxes/not-part-of-convo@seller.com/conversations/"
                        + conversation.getId()
                        + "/block");

        RestAssured.expect()
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
        RestAssured.expect()
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
        RestAssured.expect()
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
        RestAssured.expect()
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
