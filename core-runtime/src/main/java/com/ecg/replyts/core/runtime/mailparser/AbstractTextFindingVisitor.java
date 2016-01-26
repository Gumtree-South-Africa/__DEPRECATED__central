package com.ecg.replyts.core.runtime.mailparser;

import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;

import java.io.IOException;
import java.io.Reader;

abstract class AbstractTextFindingVisitor implements MailBodyVisitor {

    boolean accept(Entity e) {
        return (e.getBody() instanceof TextBody);
    }

    public void visit(Entity e, SingleBody body) {
        if (!accept(e)) {
            return;
        }
        try (Reader r = ((TextBody) e.getBody()).getReader()) {
            String textContent = CharStreams.toString(r);
            MediaType mediaType = MediaType.parse(e.getMimeType());
            // if the mail is mutable, attach the entity so that one can modify the text and the typed content can
            // write this back to the entity.
            handle(e, textContent, mediaType);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    abstract void handle(Entity e, String text, MediaType m);
}
