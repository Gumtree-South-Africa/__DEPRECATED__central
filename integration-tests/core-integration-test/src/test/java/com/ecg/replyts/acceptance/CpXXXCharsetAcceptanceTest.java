package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.google.common.io.CharStreams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertNotNull;

public class CpXXXCharsetAcceptanceTest {

    
    // Without fix there would have been a "com.ecg.replyts.core.runtime.mailparser.ParsingException: Mail contains part in charset that is not understandable"
    @Test
    public void wrongEncodingTranslatedAndSent() throws Exception {
        Mail mail = parse();
        assertNotNull(mail);
    }

    private Mail parse() throws ParsingException {
        try {
            String mailContents = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("wrong_content_encoding.eml")));
            return StructuredMail.parseMail(new ByteArrayInputStream(mailContents.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
