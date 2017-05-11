package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;

public class AttachmentSupportTest {

    @Rule
    public ReplyTsIntegrationTestRule replyTs = new ReplyTsIntegrationTestRule(ES_ENABLED);

    @Test
    public void exposesAttachmentNamesInMessage() {
        MailInterceptor.ProcessedMail deliver = replyTs.deliver(MailBuilder.aNewMail().randomAdId().randomSender().randomReceiver().attachment("foo.jpg", "random junk bytes".getBytes()));

        assertEquals(asList("foo.jpg"), deliver.getMessage().getAttachmentFilenames());
    }

    @Test
    public void noAttachmentsReturnEmptyList() {

        MailInterceptor.ProcessedMail deliver = replyTs.deliver(MailBuilder.aNewMail().randomAdId().randomSender().plainBody("foo").randomReceiver());

        assertEquals(Collections.<String>emptyList(), deliver.getMessage().getAttachmentFilenames());
    }

    @Test
    public void exposesAttachmentNamesViaApi() {
        MailInterceptor.ProcessedMail deliver = replyTs.deliver(MailBuilder.aNewMail().randomAdId().randomSender().randomReceiver().attachment("foo.jpg", "random junk bytes".getBytes()));
        RestAssured.expect().body("body.messages[0].attachments[0]", equalTo("foo.jpg")).when().get("http://localhost:" + replyTs.getHttpPort() + "/screeningv2/conversation/" + deliver.getConversation().getId());
    }
}
