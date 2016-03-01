package com.ecg.de.kleinanzeigen.negotiations;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.MessageImpl;
import org.assertj.core.api.StringAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.ecg.replyts.integration.test.AwaitMailSentProcessedListener.ProcessedMail;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * User: maldana
 * Date: 11/27/14
 * Time: 1:29 PM
 *
 * @author maldana@ebay.de
 */
public class OpenImmoDeanonymizerListenerTest {

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
        assertThat(processedMail.getOutboundMail().getFrom()).isEqualTo("realUserAddressInput@foo.de");
    }

    private void assertAnonymization(ProcessedMail processedMail) {
        assertThat(processedMail.getOutboundMail().getFrom()).startsWith("Buyer");
    }

}
