package com.ecg.comaas.ebayk.filter.emailaddress;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
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

public class EmailAddressFilterIntegrationTest {


    @Rule
    public ReplyTsIntegrationTestRule replyTS = new ReplyTsIntegrationTestRule();

    @Before
    public void setup() {
        ArrayNode addresses = JsonObjects.newJsonArray();
        addresses.add("foo@bar.com");
        replyTS.registerConfig(EmailAddressFilterFactory.IDENTIFIER, JsonObjects.builder().attr("values", addresses).attr("score", 100).build());
    }

    @Test
    public void inHeldIfConfiguredEmailAddressContaining() {
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world foo [at] bar.com"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void sentIfEmailAddressNotContaining() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("hello world IBAN DE99 - 2032 0500 - 4989 - 1234 56"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.SENT);
    }

    @Test
    public void findEmailAddressInHtmlTags() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody("<font face=\"Arial,Helvetica, sans-serif\">\n" +
                        "<b>Nachricht von:</b> rolland\n" +
                        "( <a href=\"mailto:foo@bar.com\" target=\"_blank\">mail</a>)\n" +
                        "<br><br>\n" +
                        "</font>"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void findEmailAddressInHtmlTags2() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody(" <a href=\"mailto:foo@bar.com\" >mail me</a>)"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }

    @Test
    public void findEmailAddressInSubject() {

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .subject("My mail: foo@bar.com")
                .htmlBody("Some text"));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }


    // Test ad hoc check of html mail content.
    @Test
    public void htmlMailTest() throws IOException, URISyntaxException {

        ArrayNode values = JsonObjects.newJsonArray();
        values.add("foo@bar.com");

        replyTS.registerConfig(EmailAddressFilterFactory.IDENTIFIER, JsonObjects.builder().attr("values", values).attr("score", 100).build());

        byte[] encoded = Files.readAllBytes(Paths.get(EmailAddressFilterIntegrationTest.class.getResource("/html-mail.txt").toURI()));
        String text = new String(encoded, "utf8");

        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com")
                .adId("1233343")
                .htmlBody(text));

        assertThat(processedMail.getMessage().getState()).isEqualTo(MessageState.HELD);
    }


}
