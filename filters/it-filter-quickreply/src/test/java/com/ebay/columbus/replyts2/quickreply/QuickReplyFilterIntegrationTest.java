package com.ebay.columbus.replyts2.quickreply;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

/**
 * Created by fmaffioletti on 28/07/14.
 */
public class QuickReplyFilterIntegrationTest {

    @Rule private ReplyTsIntegrationTestRule replyTsIntegrationTestRule =
                    new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
                        Properties properties = new Properties();

                        properties.put("replyts.quickreply.customHeaders", "X-CUST-QUICK_REPLY_HEADER|-10000,X-CUST-FOO|100" );
                        properties.put("replyts.quickreply.placeholders", "__!ADDRESS!__");

                        return properties;
                    }).get(), QuickReplyFilterConfiguration.class);

    @Before
    public void setUp() {
        replyTsIntegrationTestRule.registerConfig(QuickReplyFilterFactory.class, null);
    }

    @Test public void quickReplyFilterDoesNotFireWhenHeaderIsMissing() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                .htmlBody("this is a <b>word</b>! "));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test public void quickReplyTestProductionFailedMail() throws Exception {
        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                .htmlBody("Sono attualmente in viaggio d'affari in Svizzera e voglio che tu inviare\n"
                                        +
                                        "l'elemento a mio figlio in africa il costo di trasporto =C3=A8 a soli 50 Eu=\n"
                                        +
                                        "ro\n" +
                                        "tramite Poste Italiane\n"));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }
}