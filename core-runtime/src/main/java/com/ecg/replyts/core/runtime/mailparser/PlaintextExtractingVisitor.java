package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class PlaintextExtractingVisitor extends AbstractTextFindingVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(PlaintextExtractingVisitor.class);

    private final Map<Type, String> contents = new TreeMap<>();

    @Override
    void handle(Entity e, String text, MediaType m) {
        try {
            // could be text/html or text/plain
            Type type = Type.fromSubType(m.subtype().toLowerCase());
            String strippedContent = (type == Type.HTML) ? new HtmlRemover(text).getPlaintext() : text;
            contents.put(type, strippedContent);
        } catch (Exception ex) {
            LOG.warn("Could not clean up mail part", ex);
            contents.put(Type.PLAIN, text);
        }
    }

    @Override
    boolean accept(Entity e) {
        if (Mail.DISPOSITION_ATTACHMENT.equalsIgnoreCase(e.getDispositionType())) {
            return false;
        }
        return super.accept(e);
    }

    /**
     * @return The content as ordered list in the order: text/clean, text/plain, text/html.
     */
    public List<String> getContents() {
        return new ArrayList<>(contents.values());
    }

    private enum Type {
        // Prevent the order of the values,
        // This declaration defines the order of the output for multi text parts.
        CLEAN_TEXT("clean"),
        PLAIN("plain"),
        HTML("html");

        private final String subtype;

        Type(String subtype) {
            this.subtype = subtype;
        }

        static Type fromSubType(String subtypeToCheck) {
            for (Type type : values()) {
                if (subtypeToCheck.contains(type.subtype)) {
                    return type;
                }
            }
            return PLAIN;
        }
    }
}
