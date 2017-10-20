package com.ecg.replyts.acceptance;


import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static org.junit.Assert.assertEquals;

public class CloseConversationTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);

    @Test
    public void closeConversationAndExpectReplyToBeDiscarded() {
        MailInterceptor.ProcessedMail result = rule.deliver(aNewMail().from("buyer123@host.com").to("seller123@host.com").adId("999").plainBody("foo"));


        RestAssured
                .expect()
                .statusCode(200)
                .and()
                .body("status.state", CoreMatchers.is("OK"))
                .when()
                .request()
                .body("{'issuerEmail':'seller123@host.com','state': 'CLOSED'}")
                .contentType("application/json;charset=utf-8")
                .put("http://localhost:" + rule.getHttpPort() + "/screeningv2/conversation/" + result.getConversation().getId());


        // rely on conversation resuming feature - this will go into the same conversation
        MailInterceptor.ProcessedMail secondMail = rule.deliver(aNewMail().from("buyer123@host.com").to("seller123@host.com").adId("999").plainBody("foo"));
        assertEquals(MessageState.DISCARDED, secondMail.getMessage().getState());
        assertEquals(ConversationState.CLOSED, secondMail.getConversation().getState());

    }
}
