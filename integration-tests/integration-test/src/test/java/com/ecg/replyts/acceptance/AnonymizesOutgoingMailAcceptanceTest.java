package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymizesOutgoingMailAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void anonymizesMailsCorrectly() throws Exception {
        rule.deliver(
                aNewMail()
                        .from("sam@gmail.com")
                        .to("max@mail.com")
                        .adId("1234")
                        .plainBody("Hello max, I want to buy your stuff")
        );
        MimeMessage conversationStarterMail = rule.waitForMail();

        String sender = conversationStarterMail.getFrom()[0].toString();
        String receiver = conversationStarterMail.getRecipients(Message.RecipientType.TO)[0].toString();

        String senderPattern = "^(Buyer|Seller)\\.[0-9a-zA-Z]+@test-platform\\.com";
        assertTrue("Sender " + sender + " must match pattern when being cloaked " + senderPattern, sender.matches(senderPattern));
        assertEquals("max@mail.com", receiver);
    }


    @Test
    public void supportsRepliesToAnonymizedMailAddress() throws Exception {

        rule.deliver(aNewMail().from("buyer@host.com").to("seller@host.com").adId("123").plainBody("hello world!"));

        MimeMessage anonymizedInitialMail = rule.waitForMail();

        String anonymizedBuyer = anonymizedInitialMail.getFrom()[0].toString();

        rule.deliver(aNewMail().from("seller@host.com").to(anonymizedBuyer).plainBody("reply to contact poster mail"));

        MimeMessage anonymizedReply = rule.waitForMail();

        String receiver = anonymizedReply.getRecipients(Message.RecipientType.TO)[0].toString();

        assertEquals("buyer@host.com", receiver);
    }

}
