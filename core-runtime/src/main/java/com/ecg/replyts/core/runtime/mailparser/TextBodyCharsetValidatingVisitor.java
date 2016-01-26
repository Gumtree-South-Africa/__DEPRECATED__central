package com.ecg.replyts.core.runtime.mailparser;

import com.google.common.base.Optional;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * Mime4J ignores mail body charsets when parsing mails. However, we can't hand
 */
class TextBodyCharsetValidatingVisitor implements MailBodyVisitor {

    private Optional<UnsupportedEncodingException> lastException = Optional.absent();

    @Override
    public void visit(Entity e, SingleBody body) {
        if (e.getBody() instanceof TextBody) {
            try (Reader r = ((TextBody) e.getBody()).getReader()) { // NOSONAR
            } catch (UnsupportedEncodingException ex) {
                lastException = Optional.of(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Optional<UnsupportedEncodingException> getEncodingErrors() {
        return lastException;
    }
}
