import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

/**
 * Created by mdarapour.
 */
public class ReadRepairIndexerIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("persistence.strategy", "riak");

        return properties;
    }).get());

    @Test
    public void testStartup() {
        rule.assertNoMailArrives();

        Conversation conversation = rule.deliver(
                aNewMail()
                        .from("buyer1@buyer.com")
                        .to("seller1@seller.com")
                        .adId("232323")
                        .plainBody("First contact from buyer.")
        ).getConversation();

        rule.waitForMail();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .when()
                .request()
                .header("Content-Type", "application/json")
                .get("http://localhost:" + rule.getHttpPort() + "/readrepair-indexer/conversations/siblings/" + conversation.getId());
    }
}
