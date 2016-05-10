package com.ecg.au.gumtree.replyts.threading;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;
import javax.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mdarapour
 */
public class MessageThreadingPostprocessorIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void addsReferencesHeadersIfDoesNotExists() throws Exception {
        Mail mail;
        MimeMessage anonymizedMail;
        String sender;
        List<String> messageIds = new ArrayList<String>();

        mail = rule.deliver(MailBuilder.aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("123")
                .htmlBody("first from buyer")).getOutboundMail();

        assertFalse(mail.containsHeader(Mail.REFERENCES_HEADER));
        assertFalse(mail.containsHeader(Mail.IN_REPLY_TO_HEADER));

        anonymizedMail = rule.waitForMail();
        sender = anonymizedMail.getFrom()[0].toString();
        messageIds.add(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER));

        mail = rule.deliver(MailBuilder.aNewMail()
                .from("seller@bar.com")
                .to(sender)
                .adId("123")
                .htmlBody("second from seller")).getOutboundMail();

        anonymizedMail = rule.waitForMail();
        sender = anonymizedMail.getFrom()[0].toString();
        messageIds.add(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER));

        assertTrue(mail.containsHeader(Mail.REFERENCES_HEADER));
        assertTrue(mail.containsHeader(Mail.IN_REPLY_TO_HEADER));
        assertFalse(mail.getUniqueHeader(Mail.REFERENCES_HEADER).contains(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER)));
        assertEquals(1, getReferences(mail.getUniqueHeader(Mail.REFERENCES_HEADER)).length);
        assertEquals(toUnifiedFormat(messageIds.get(0)), getReferences(mail.getUniqueHeader(Mail.REFERENCES_HEADER))[0]);
        assertEquals(messageIds.get(0), mail.getUniqueHeader(Mail.IN_REPLY_TO_HEADER));

        mail = rule.deliver(MailBuilder.aNewMail()
                .from("buyer@foo.com")
                .to(sender)
                .adId("123")
                .htmlBody("third from buyer")).getOutboundMail();

        anonymizedMail = rule.waitForMail();
        sender = anonymizedMail.getFrom()[0].toString();
        messageIds.add(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER));

        assertTrue(mail.containsHeader(Mail.REFERENCES_HEADER));
        assertFalse(mail.getUniqueHeader(Mail.REFERENCES_HEADER).contains(mail.getUniqueHeader(Mail.MESSAGE_ID_HEADER)));
        assertEquals(2, getReferences(mail.getUniqueHeader(Mail.REFERENCES_HEADER)).length);
        assertEquals(toUnifiedFormat(messageIds.get(0)), getReferences(mail.getUniqueHeader(Mail.REFERENCES_HEADER))[0]);
        assertEquals(toUnifiedFormat(messageIds.get(1)), getReferences(mail.getUniqueHeader(Mail.REFERENCES_HEADER))[1]);
        assertEquals(messageIds.get(1), mail.getUniqueHeader(Mail.IN_REPLY_TO_HEADER));
    }

    @Test
    public void testGetReferences() {
        String[] references;
        references = getReferences("<1hpzxl5r9helr3n8ypfmbo6pgqc@test-platform.com><1hpzxl5r9helr3n8ypfmbo6pecg@test-platform.com>");
        assertEquals(2, references.length);
        assertEquals("1hpzxl5r9helr3n8ypfmbo6pgqc@test-platform.com", references[0]);
        assertEquals("1hpzxl5r9helr3n8ypfmbo6pecg@test-platform.com", references[1]);
        references = getReferences("<1hpzxl5r9helr3n8ypfmbo6pgqc@test-platform.com>");
        assertEquals(1, references.length);
        assertEquals("1hpzxl5r9helr3n8ypfmbo6pgqc@test-platform.com", references[0]);
    }

    private String[] getReferences(String value) {
        String[] result = value.split(">.*<");
        for(int i = 0; i < result.length; i++)
            result[i] = toUnifiedFormat(result[i]);
        return result;
    }

    private String toUnifiedFormat(String messageId) {
        return messageId.replaceAll(">|<","");
    }
}
