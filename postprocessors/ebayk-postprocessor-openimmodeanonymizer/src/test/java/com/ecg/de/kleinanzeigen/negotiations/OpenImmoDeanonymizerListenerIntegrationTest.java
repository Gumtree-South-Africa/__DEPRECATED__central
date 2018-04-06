package com.ecg.de.kleinanzeigen.negotiations;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: maldana
 * Date: 11/27/14
 * Time: 1:29 PM
 *
 * @author maldana@ebay.de
 */
public class OpenImmoDeanonymizerListenerIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule itRule = new ReplyTsIntegrationTestRule();

    @Test
    public void deannoymizationForOpenImmo() {
        ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .adId("123")
                .from("realUserAddressInput@foo.de")
                .to("to@foo.de")
                .plainBody("foo")
                .customHeader("Ad-Api-User-Id", "20002"));

        assertNoAnonymization(processedMail);
    }

    @Test
    public void noDeannoymizationForNonOpenImmo() {
        ProcessedMail processedMail = itRule.deliver(MailBuilder.aNewMail()
                .adId("123")
                .from("realUserAddressInput@foo.de")
                .to("to@foo.de")
                .plainBody("foo")
                .customHeader("Ad-Api-User-Id", "2"));

        assertAnonymization(processedMail);
    }

    private void assertNoAnonymization(ProcessedMail processedMail) {
        assertThat(processedMail.getOutboundMail().getReplyTo()).isEqualTo("realUserAddressInput@foo.de");
        assertThat(processedMail.getOutboundMail().getFrom()).isEqualTo("noreply-immobilien@ebay-kleinanzeigen.de");
    }

    private void assertAnonymization(ProcessedMail processedMail) {
        assertThat(processedMail.getOutboundMail().getFrom()).startsWith("Buyer");
        assertThat(processedMail.getOutboundMail().getReplyTo()).isNull();
    }

}
