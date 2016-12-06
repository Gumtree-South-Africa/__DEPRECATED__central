package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.stream.MimeConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class MimeHelper {

    private static final int MAX_HEADER_LENGTH = 30_000;

    // Temporary fix for COMAAS-310. Once the mime4j issue has been resolved, remove this line and revert back to the
    // default limit of 1,000 characters for body lines.
    private static final int MAX_LINE_LEN = -1;

    private MimeHelper() {

    }

    static Message parseAndConsume(InputStream i) throws ParsingException, IOException {
        try (InputStream ii = i) {
            DefaultMessageBuilder messageBuilder = new DefaultMessageBuilder();
            MimeConfig mimeConfig = new MimeConfig();

            // Some email providers (like Hotmail) send a References header that's >10,000
            // characters long (which is the default limit in MimeConfig)
            // Our X-Track-referrer: is also sometimes up to 1600 characters long.
            // This doesn't actually work: https://issues.apache.org/jira/browse/MIME4J-256
            mimeConfig.setMaxLineLen(MAX_LINE_LEN);
            mimeConfig.setMaxHeaderLen(MAX_HEADER_LENGTH);

            messageBuilder.setMimeEntityConfig(mimeConfig);
            return messageBuilder.parseMessage(ii);
        } catch (MimeIOException e) {
            throw new ParsingException("Could not parse message", e);
        }
    }

    static Message copy(Message m) {
        return new DefaultMessageBuilder().copy(m);
    }

    static void write(Message m, OutputStream o) throws IOException {
        new DefaultMessageWriter().writeMessage(m, o);
    }

}
