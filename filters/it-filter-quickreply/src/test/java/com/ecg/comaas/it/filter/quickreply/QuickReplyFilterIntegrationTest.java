package com.ecg.comaas.it.filter.quickreply;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

public class QuickReplyFilterIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(createProperties());

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_IT);
        properties.put("replyts.quickreply.customHeaders", "X-CUST-QUICK_REPLY_HEADER|-10000,X-CUST-FOO|100" );
        properties.put("replyts.quickreply.placeholders", "__!ADDRESS!__");
        return properties;
    }

    @Before
    public void setUp() {
        rule.registerConfig(QuickReplyFilterFactory.IDENTIFIER, null);
    }

    @Test public void quickReplyFilterDoesNotFireWhenHeaderIsMissing() throws Exception {
        MailInterceptor.ProcessedMail processedMail = rule
                        .deliver(aNewMail().adId("1234").from("foo@bar.com").to("bar@foo.com")
                                .htmlBody("this is a <b>word</b>! "));
        Assert.assertEquals(0, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test public void quickReplyTestProductionFailedMail() throws Exception {
        MailInterceptor.ProcessedMail processedMail = rule
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
