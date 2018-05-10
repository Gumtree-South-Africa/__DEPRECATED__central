package com.ecg.messagecenter.ebayk.webapi;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class GdprForV1MbAcceptanceTest {
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties(), null, 20, ES_ENABLED, "cassandra_schema.cql", "cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_EBAYK);
        properties.put("replyts.tenant", "ebayk");
        properties.put("message.synchronizer.enabled", "false");
        properties.put("hazelcast.password", "123");
        properties.put("hazelcast.port.increment", "true");
        properties.put("active.dc", "localhost");
        properties.put("region", "localhost");
        properties.put("persistence.cassandra.conversation.class", "com.ecg.messagecenter.ebayk.persistence.ConversationThread");
        properties.put("persistence.strategy", "cassandra");
        properties.put("persistence.cassandra.consistency.read", "LOCAL_QUORUM");
        properties.put("persistence.cassandra.consistency.write", "LOCAL_QUORUM");
        properties.put("persistence.cassandra.core.dc", "datacenter1");
        properties.put("persistence.cassandra.core.endpoint", "localhost:9042");
        properties.put("persistence.riak.connectionPoolSizePerRiakHost", "1");
        properties.put("persistence.riak.connectionTimeoutMs", "1");
        properties.put("persistence.riak.datacenter.primary.hosts", "replyts.dev.kjdev.ca");
        properties.put("persistence.riak.idleConnectionTimeoutMs", "1");
        properties.put("persistence.riak.maxConnectionsToRiakCluster", "1");
        properties.put("persistence.riak.pb.port", "8087");
        properties.put("persistence.riak.requestTimeoutMs", "1");
        properties.put("riak.cluster.monitor.enabled", "false");
        return properties;
    }

    @Test
    public void conversationCanBeClosedCorrectly() throws Exception {
        // setup a conversation
        String convId = testRule.deliver(
                aNewMail()
                        .from("sam@example.com")
                        .to("max@example.com")
                        .adId("1234")
                        .plainBody("Hello max, I want to buy your stuff")
        ).getConversation().getId();

        String anonymizedBuyerEmail = testRule.waitForMail().getFrom()[0].toString();

        assertThat(conversationsInV1MessageboxForUser("sam@example.com"), is(1));
        assertThat(conversationsInV1MessageboxForUser("max@example.com"), is(1));

        // close the conversation
        RestAssured
                .given()
                .body("{ 'state':'CLOSED', 'deleteForIssuer':'true', 'issuerEmail':'sam@example.com'}")
                .contentType("application/json")
                .expect()
                .statusCode(200)
                .put("http://localhost:" + testRule.getHttpPort() + "/screeningv2/conversation/" + convId);

        // assert that messagebox is empty for DDR initiating user
        assertThat(conversationsInV1MessageboxForUser("sam@example.com"), is(0));

        // assert that other party is not affected
        assertThat(conversationsInV1MessageboxForUser("max@example.com"), is(1));

        // send a new message by user 30
        testRule.deliver(
                aNewMail()
                        .from("max@example.com")
                        .to(anonymizedBuyerEmail)
                        .adId("1234")
                        .plainBody("Hi sam, are you still there?"));

        // no new conversation should be in the messageboxes
        assertThat(conversationsInV1MessageboxForUser("sam@example.com"), is(0));
        assertThat(conversationsInV1MessageboxForUser("max@example.com"), is(1));
    }

    private int conversationsInV1MessageboxForUser(String userEmail) {
        String response = RestAssured.given()
                .expect()
                .statusCode(200)
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/" + userEmail)
                .asString();

        return JsonPath.from(response).getInt("body.conversations.size");
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
                .get("http://localhost:" + testRule.getHttpPort() + "/ebayk-msgcenter/postboxes/seller1@seller.com");
    }

}
