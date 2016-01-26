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

    public static final int MAX_HEADER_LENGTH = 30_000;

    private MimeHelper() {

    }

    static Message parseAndConsume(InputStream i) throws ParsingException, IOException {
        try (InputStream ii = i) {
            DefaultMessageBuilder messageBuilder = new DefaultMessageBuilder();
            MimeConfig mimeConfig = new MimeConfig();

            // Some email providers (like Hotmail) send a References header that's >10,000
            // characters long (which is the default limit in MimeConfig)
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
