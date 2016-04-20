package com.ecg.replyts.core.runtime.mailfixers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.processing.MessageFixer;
import com.ecg.replyts.core.runtime.mailparser.MailBodyVisitingClient;
import com.ecg.replyts.core.runtime.mailparser.MailBodyVisitor;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.field.ContentTypeFieldLenientImpl;
import org.apache.james.mime4j.stream.DefaultFieldBuilder;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.ByteArrayBuffer;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.io.InputStream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

/**
 * Attempts to fix Content-Type headers in a message.
 *
 * Some mail clients send a Content-Type header that's invalid. ReplyTS processes the
 * message, but when it comes time to send it, JavaMail throws an exception.
 * This class validates every Content-Type of every MIME part, tries to guess the type
 * if it's not parsable, and resets it to the detected one. Tika will use
 * application/octet-stream as a fallback type when guessing fails.
 */
public class BrokenContentTypeFix implements MessageFixer {
    private static final Logger LOG = LoggerFactory.getLogger(BrokenContentTypeFix.class);

    private static Tika CONTENT_TYPE_DETECTOR = new Tika();

    private final Timer executionTimer = newTimer("fixers.content-type.execution");

    @Override
    public void applyIfNecessary(Message mail, Exception originalException) {
        if (originalException != null && originalException.getMessage().contains("javax.mail.internet.ParseException")) {
            applyIfNecessary(mail);
        }
    }

    @Override
    public void applyIfNecessary(Message mail) {
        MailBodyVisitingClient mailBodyVisitingClient = new MailBodyVisitingClient(mail);

        try (Timer.Context ignored = executionTimer.time()) {
            mailBodyVisitingClient.visit(new ContentTypeMailBodyVisitor());
        }
    }

    private static class ContentTypeMailBodyVisitor implements MailBodyVisitor {
        private final Counter detectionFailureCounter = newCounter("fixers.content-type.detection-failures");
        private final Counter rewriteFailureCounter = newCounter("fixers.content-type.header-rewrite-failures");

        // Mime type are usually pretty short
        // application/vnd.openxmlformats-officedocument.presentationml.presentation is common,
        // but pad a bit just to be safe (also, account for header name: "Content-Type: ")
        static final int MAX_REBUILT_CONTENT_TYPE_HEADER_LENGTH = 150;

        @Override
        public void visit(Entity entity, SingleBody body) {
            ContentTypeField contentTypeField = (ContentTypeField) entity.getHeader().getField("Content-Type");
            if (contentTypeField == null) {
                return;
            }

            try {
                new ContentType(contentTypeField.getBody());
            } catch (ParseException parseException) {
                guessAndRewriteContentTypeHeader(entity, body);
            }
        }

        private void guessAndRewriteContentTypeHeader(Entity entity, SingleBody body) {
            String guessedContentType;
            try {
                try (InputStream inputStream = body.getInputStream()) {
                    guessedContentType = CONTENT_TYPE_DETECTOR.detect(inputStream);
                }
            } catch (IOException e) {
                detectionFailureCounter.inc();
                LOG.warn("Exception during content type detection", e);
                return;
            }

            rewriteContentTypeHeader(entity, guessedContentType);
        }

        private void rewriteContentTypeHeader(Entity entity, String newContentType) {
            DefaultFieldBuilder fieldBuilder = new DefaultFieldBuilder(MAX_REBUILT_CONTENT_TYPE_HEADER_LENGTH);

            try {
                // Charset and filename parameters aren't preserved in the generated Content-Type, because:
                // a) charset is only set on text parts, which usually have a valid header anyway
                // b) filename is typically set in the Content-Disposition header
                // The above are just observations. Feel free to add support if necessary.

                fieldBuilder.append(new ByteArrayBuffer(("Content-Type: " + newContentType).getBytes(), false));
                RawField rawField = fieldBuilder.build();
                ContentTypeField resultField = ContentTypeFieldLenientImpl.PARSER.parse(rawField, null);
                entity.getHeader().setField(resultField);
            } catch (MimeException e) {
                rewriteFailureCounter.inc();
                LOG.warn("Failed to rewrite the Content-Type header to [{}]", newContentType, e);
            }
        }
    }
}
