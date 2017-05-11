package com.ecg.replyts.acceptance.test.core;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;

public class SetCustomValueTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);

    private static final Logger LOG = LoggerFactory.getLogger(SetCustomValueTest.class);


    @Test
    public void addsNewIntegrationTests() {
        MailInterceptor.ProcessedMail deliver = rule.deliver(MailBuilder.aNewMail().randomAdId().randomSender().randomReceiver().htmlBody("asfdasdf"));

        String host = String.format("http://localhost:%s/screeningv2/", rule.getHttpPort());

        RestAssured.expect().statusCode(200).request().contentType("application/json").body("{'key': 'newKey', 'value': '123'}").put(host+"conversation/"+deliver.getConversation().getId()+"/customValue");


        RestAssured.expect().statusCode(200).and().body("body.conversationHeaders.newKey", CoreMatchers.equalTo("123")).request().get(host+"conversation/"+deliver.getConversation().getId());

    }
}
