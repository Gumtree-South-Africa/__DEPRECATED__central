package com.ecg.replyts.core.runtime.persistence;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.commands.ModerateMessageCommand;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

public class HeldMailRepositoryIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();

    @Test
    public void testChangeState() {
        // Mark all incoming messages as HELD

        testRule.registerConfig(AlwaysHoldIntegrationFilterFactory.IDENTIFIER, JsonObjects.builder().build());

        // Send a message and confirm it is indeed HELD

        MailInterceptor.ProcessedMail processedMail = testRule.deliver(MailBuilder.aNewMail()
          .from("foo@bar.com").to("bar@foo.com").adId("1234567").htmlBody("Hello world!"));

        assertEquals(MessageState.HELD, processedMail.getMessage().getState());

        // Use the MessageController endpoint to mark it as GOOD

        String url = "http://localhost:" + testRule.getHttpPort() + "/screeningv2" + ModerateMessageCommand.MAPPING
          .replace("{conversationId}", processedMail.getConversation().getId())
          .replace("{messageId}", processedMail.getMessage().getId());

        // Should return OK the first time and ENTITY_OUTDATED the second time

        for (int i = 0; i < 2; i++) {
            given()
              .contentType("application/json")
              .body("{ \"currentMessageState\": \"HELD\", \"newMessageState\": \"GOOD\", \"editor\": \"Jane\" }")
            .when()
              .post(url).then()
              .statusCode(200)
              .body("status.state", equalTo(i == 0 ? "SUCCESS" : "ENTITY_OUTDATED"));
        }
    }

    @Component
    public static class AlwaysHoldIntegrationFilterFactory implements FilterFactory {

        private static final String IDENTIFIER = "com.ecg.replyts.core.runtime.persistence.AlwaysHoldIntegrationFilterFactory";

        @Nonnull
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            return context -> Collections.singletonList(new FilterFeedback("Foo", "Bar", 0, FilterResultState.HELD));
        }

        @Override
        public String getIdentifier() {
            return IDENTIFIER;
        }
    }
}
