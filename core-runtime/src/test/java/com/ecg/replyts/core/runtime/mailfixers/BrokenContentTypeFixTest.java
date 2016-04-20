package com.ecg.replyts.core.runtime.mailfixers;

import com.google.common.io.CharStreams;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.MailSendException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class BrokenContentTypeFixTest {
    private BrokenContentTypeFix brokenContentTypeFix;

    @Before
    public void setUp() throws Exception {
        brokenContentTypeFix = new BrokenContentTypeFix();
    }

    @Test
    public void totallyBrokenContentType_fixed() throws Exception {
        // The attachment here is a PNG image of a company logo
        Message message = readEmailFrom("/mailfixer/content-type-symbol.eml");

        String result = fixAndGetEmailAsString(message);
        assertThat(result, containsString("Content-Type: image/png"));
        assertThat(result, not(containsString("Content-Type: %3C")));
    }

    @Test
    public void doubleContentType_fixed() throws Exception {
        // MS Outlook loves double content-types. Its favourite is "image/jpeg; image/spj".
        Message message = readEmailFrom("/mailfixer/content-type-double.eml");

        String result = fixAndGetEmailAsString(message);
        assertThat(result, containsString("Content-Type: image/jpeg"));
        assertThat(result, not(containsString("image/spj")));
    }

    @Test
    public void emptyContentType_detected() throws Exception {
        Message message = readEmailFrom("/mailfixer/content-type-empty.eml");

        String result = fixAndGetEmailAsString(message);
        assertThat(result, containsString("Content-Type: image/jpeg"));
    }

    @Test
    public void undetectableContentType_defaultUsed() throws Exception {
        Message message = readEmailFrom("/mailfixer/content-type-undetectable.eml");

        String result = fixAndGetEmailAsString(message);
        assertThat(result, containsString("Content-Type: application/octet-stream"));
    }

    private Message readEmailFrom(String emailFileName) throws IOException {
        String msgContent;
        try(InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(emailFileName))) {
            msgContent = CharStreams.toString(reader);
        }

        return new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(
                msgContent.getBytes()
        ));
    }

    private String fixAndGetEmailAsString(Message message) throws IOException {
        brokenContentTypeFix.applyIfNecessary(
                message,
                new MailSendException("javax.mail.internet.ParseException: Expected '=', got \"/\"")
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new DefaultMessageWriter().writeMessage(message, outputStream);
        return outputStream.toString();
    }
}
