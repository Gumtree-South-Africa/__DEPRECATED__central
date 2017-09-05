package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

class TextBodyCharsetValidatingVisitor implements MailBodyVisitor {

    private Optional<UnsupportedEncodingException> lastException = Optional.empty();

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
