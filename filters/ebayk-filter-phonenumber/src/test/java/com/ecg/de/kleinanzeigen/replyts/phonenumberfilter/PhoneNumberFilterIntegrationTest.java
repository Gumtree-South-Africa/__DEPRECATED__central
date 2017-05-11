package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneNumberFilterIntegrationTest {


    @Rule
    public ReplyTsIntegrationTestRule replyTS = new ReplyTsIntegrationTestRule();

    @Before
    public void setup() {
        ArrayNode numbers = JsonObjects.newJsonArray();
        numbers.add("015156035123");
        replyTS.registerConfig(PhoneNumberFilterFactory.class, JsonObjects.builder().attr("numbers", numbers).attr("score", 100).build());
    }

    @Test
    public void inHeldIfConfiguredPhoneNumberContaining() {
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world (0151)  560 / 35123 "));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void bugDontKeepLastResult() {
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world 015156035123"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);

        processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world "));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.SENT);
    }

    @Test
    public void sentIfPhonenNumberNotContaining() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world IBAN DE99 - 2032 0500 - 4989 - 1234 56"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.SENT);
    }

    @Test
    public void findPhoneNumberInHtmlTags() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("<font face=\"Arial,Helvetica, sans-serif\">\n" +
                        "<b>Nachricht von:</b> rolland\n" +
                        "(Tel.: <a href=\"tel:%2B4915156035123\" target=\"_blank\" value=\"+4915156035123\">+4915156035123</a>)\n" +
                        "<br><br>\n" +
                        "</font>"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void findPhoneNumberInHtmlTags2() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("(Tel.: <a href=\"tel:%2B4915156035123\" target=\"_blank\" value=\"+4915156035123\">Call me</a>)"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void findPhoneNumberInSubject() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .subject("tel: 015156035123")
                .htmlBody("Some text"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }


    // Test ad hoc check of html mail content.
    @Test
    public void htmlMailTest() throws IOException, URISyntaxException {

        ArrayNode numbers = JsonObjects.newJsonArray();
        numbers.add("+4915151234567");

        replyTS.registerConfig(PhoneNumberFilterFactory.class, JsonObjects.builder().attr("numbers", numbers).attr("score", 100).build());

        byte[] encoded = Files.readAllBytes(Paths.get(PhoneNumberFilterIntegrationTest.class.getResource("/html-mail.txt").toURI()));
        String text = new String(encoded, "utf8");

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody(text));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }


}
