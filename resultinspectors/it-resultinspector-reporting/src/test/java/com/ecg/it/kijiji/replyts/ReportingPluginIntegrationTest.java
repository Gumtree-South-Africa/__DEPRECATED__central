package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

/**
 * Created by fmaffioletti on 28/07/14.
 */
public class ReportingPluginIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
                    new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
                        Properties properties = new Properties();
                        return properties;
                    }).get(), TestConfiguration.class, ReportingResultInspectorConfiguration.class);

    @Before public void setUp() {
        replyTsIntegrationTestRule.registerConfig(ReportingResultInspectorFactory.class, null);
        replyTsIntegrationTestRule.registerConfig(FakeWordfilterPluginFactory.class, null);
    }

    @Test public void testSuccess() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("this is a test"));
        Assert.assertEquals(MessageState.SENT, processedMail.getMessage().getState());
        Assert.assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test public void testBlock() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("2").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("this is a block"));
        Assert.assertEquals(MessageState.SENT, processedMail.getMessage().getState());
        Assert.assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public FilterFactory getFilterFactory() {
            return new FakeWordfilterPluginFactory();
        }
    }

}
    