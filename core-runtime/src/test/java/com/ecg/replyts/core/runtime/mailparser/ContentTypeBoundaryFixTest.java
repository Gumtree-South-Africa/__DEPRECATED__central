package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.apache.james.mime4j.dom.field.FieldName.CONTENT_TYPE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ContentTypeBoundaryFixTest {
    ContentTypeBoundaryFix contentTypeBoundaryFix;

    @Before
    public void setUp() throws Exception {
        contentTypeBoundaryFix = new ContentTypeBoundaryFix();
    }

    @Test
    public void rewritesMultipartBoundary() throws Exception {
        Message message = new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(
                ("From: foo@bar.com\n" +
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
                        "<html><body>hello world</body></html>").getBytes()));

        contentTypeBoundaryFix.applyIfNecessary(message);

        assertThat(message.getHeader().getField(CONTENT_TYPE).getBody(), not(containsString("someoneebaycom")));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new DefaultMessageWriter().writeMessage(message, os);
        assertThat(os.toString(), not(containsString("someoneebaycom")));
    }

    @Test
    public void noContentType_notRewritten() throws Exception {
        Message message = new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(
                ("From: foo@bar.com\n" +
                        "To: foo@bar.com\n" +
                        "Delivered-To: foo@bar.com\n" +
                        "Subject: asdfasdf\n" +
                        "\n" +
                        "hello world").getBytes()));

        contentTypeBoundaryFix.applyIfNecessary(message);

        assertNull(message.getHeader().getField(CONTENT_TYPE));
    }
}
