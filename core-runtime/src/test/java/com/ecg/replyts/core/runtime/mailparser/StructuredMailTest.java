package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class StructuredMailTest {


    @Test(expected = ParsingException.class)
    public void wrapsUnparsableMailAddressToParsingException() throws ParsingException {
        parse("Delivered-To: Pascal Kontzak<bluetrox@gmx.de<mailto:bluetrox@gmx.de>>\nTo: foo@bar.com\nSubject: asdfasdf\n\nhello world");

    }

    @Test
    public void extractsAllToAddresses() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nSubject: asdfasdf\n\nhello world");
        assertTrue(mail.getTo().containsAll(Lists.newArrayList("foo@bar.com", "a@b.com", "c@d.com")));
    }

    @Test
    public void handlesMediaTypesThatEndWithASemicolon() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: text/plain; charset=\"iso-8859-1\";\nSubject: asdfasdf\n\nhello world");
        mail.getMainContentType();
    }

    @Test
    public void returnsCorrectDeliveredTo() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: text/plain; charset=\"iso-8859-1\";\nSubject: asdfasdf\n\nhello world");
        assertEquals("asf@ac.com", mail.getDeliveredTo());
    }

    @Test
    public void extractsAttachmentNames() throws IOException, ParsingException {
        Mail mail = parse();
        assertEquals(asList("Screen Shot 2013-08-23 at 10.09.19.png"), mail.getAttachmentNames());
    }

    @Test
    public void retrievesAttachmentContents() throws IOException, ParsingException {
        TypedContent<byte[]> attachment = parse().getAttachment("Screen Shot 2013-08-23 at 10.09.19.png");
        assertEquals("image/png", attachment.getMediaType().toString());
        assertEquals(71741, attachment.getContent().length);
    }


    @Test
    public void extractsMainContentTypeWithoutSemicolon() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar; charset=\"iso-8859-1\"\nSubject: asdfasdf\n\nhello world");
        assertEquals(mail.getMainContentType().type(), "foo");
        assertEquals(mail.getMainContentType().subtype(), "bar");
        assertEquals(mail.getMainContentType().charset().get().name(), "ISO-8859-1");
    }


    @Test
    public void extractsMainContentTypeWithSemicolon() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar; charset=\"iso-8859-1\";\nSubject: asdfasdf\n\nhello world");
        assertEquals(mail.getMainContentType().type(), "foo");
        assertEquals(mail.getMainContentType().subtype(), "bar");
        assertEquals(mail.getMainContentType().charset().get().name(), "ISO-8859-1");
    }


    @Test
    public void extractsMainContentTypeWithoutSemicolonNoCharset() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar\nSubject: asdfasdf\n\nhello world");
        assertEquals(mail.getMainContentType().type(), "foo");
        assertEquals(mail.getMainContentType().subtype(), "bar");
    }


    @Test
    public void extractsMainContentTypeWithSemicolonNoCharset() throws ParsingException {
        Mail mail = parse("From: foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar;\nSubject: asdfasdf\n\nhello world");
        assertEquals(mail.getMainContentType().type(), "foo");
        assertEquals(mail.getMainContentType().subtype(), "bar");
    }

    @Test
    public void rewritesContentTypeBoundary() throws Exception {
        Mail mail = parse("From: foo@bar.com\n" +
                "To: foo@bar.com\n" +
                "Delivered-To: foo@bar.com\n" +
                "Content-Type: multipart/alternative;\n" +
                "        boundary=\"_000_D163B3112A4Esomeoneebaycom_\"\n" +
                "Subject: asdfasdf\n" +
                "\n" +
                "--_000_D163B3112A4Esomeoneebaycom_\n" +
                "Content-Type: text/plain; charset=\"UTF-8\"\n" +
                "Content-Transfer-Encoding: quoted-printable\n\n" +
                "hello world\n\n" +
                "--_000_D163B3112A4Esomeoneebaycom_\n" +
                "Content-Type: text/html; charset=\"UTF-8\"\n" +
                "Content-Transfer-Encoding: quoted-printable\n\n" +
                "<html><body>hello world</body></html>");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mail.writeTo(outputStream);

        assertThat(outputStream.toString(), not(containsString("_000_D163B3112A4Esomeoneebaycom_")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsToRetrieveMissingAttachment() {
            parse().getAttachment("asdf");
    }

    @Test
    public void parsesBrokenFromHeaders() throws Exception {
        // Some iOS mail clients send these kinds of headers
        Mail mail = parse("From: <      <foo@bar.com> >\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar\nSubject: asdfasdf\n\nhello world");
        assertEquals(mail.getFrom(), "foo@bar.com");

        try {
            // Haven't seen this yet, so expected to fail
            parse("From: <      <foo@bar.com\nTo: foo@bar.com,a@b.com,c@d.com\nDelivered-To: asf@ac.com\nContent-Type: foo/bar\nSubject: asdfasdf\n\nhello world");
            fail("should fail to parse From: <      <foo@bar.com");
        } catch (ParsingException e) {
            // expected
        }
    }

    private Mail parse() {
        try {
            return parse(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/mail-with-attachment.eml"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Mail parse(String mailContents) throws ParsingException {
        try {
            return StructuredMail.parseMail(new ByteArrayInputStream(mailContents.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
