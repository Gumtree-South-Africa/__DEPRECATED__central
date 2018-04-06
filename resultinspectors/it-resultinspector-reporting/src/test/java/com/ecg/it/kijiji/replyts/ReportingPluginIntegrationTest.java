package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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


/**
 * We set this test as ignored because it relies on an external service (statsd) which is not available in comaas
 * environment. Once we'll decide where to install this external service and to which testing profile this test will
 * belong to, we can re-enable it again.
 */

public class ReportingPluginIntegrationTest {

    @Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
                    new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
                        Properties properties = new Properties();
                        return properties;
                    }).get(), TestConfiguration.class, ReportingResultInspectorConfiguration.class);

    @Before public void setUp() {
        replyTsIntegrationTestRule.registerConfig(ReportingResultInspectorFactory.IDENTIFIER, null);
        replyTsIntegrationTestRule.registerConfig(FakeWordfilterPluginFactory.IDENTIFIER, null);
    }

    @Ignore
    @Test public void testSuccess() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1").from("foo@bar.com").to("bar@foo.com")
                                        .htmlBody("this is a test"));
        Assert.assertEquals(MessageState.SENT, processedMail.getMessage().getState());
        Assert.assertEquals(1, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Ignore
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
