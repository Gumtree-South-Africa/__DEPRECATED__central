package com.ecg.replyts.acceptance;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.ecg.replyts.integration.test.filter.SubjectKeywordFilterFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageFilteringAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();


    @Test
    public void doesSendMailIfNoFilterRunningButFilterFactoryKnown() {
        AwaitMailSentProcessedListener.ProcessedMail output = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").subject("DROPPED").adId("123").htmlBody("foo"));

        assertEquals(MessageState.SENT, output.getMessage().getState());
    }

    @Test
    public void doesNotDeliverWhenFilterBlocksMail() throws Exception {

        rule.registerConfig(SubjectKeywordFilterFactory.class, JsonObjects.builder().attr("foo", "bar").build());

        AwaitMailSentProcessedListener.ProcessedMail output = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").subject("DROPPED").adId("123").htmlBody("foo"));

        assertEquals(MessageState.BLOCKED, output.getMessage().getState());
        rule.assertNoMailArrives();
    }

    @Test
    public void stopsFilterOnceFilterConfigDeleted() throws Exception {
        Configuration.ConfigurationId newConfig = rule.registerConfig(SubjectKeywordFilterFactory.class, JsonObjects.builder().attr("foo", "bar").build());

        MailBuilder mail = MailBuilder.aNewMail()
                .from("foo@bar.com")
                .to("bar@foo.com")
                .subject("DROPPED")
                .adId("123")
                .htmlBody("foo");

        AwaitMailSentProcessedListener.ProcessedMail output = rule.deliver(mail);
        assertEquals(MessageState.BLOCKED, output.getMessage().getState());
        rule.deleteConfig(newConfig);

        output = rule.deliver(mail);
        assertEquals(MessageState.SENT, output.getMessage().getState());
    }
}
