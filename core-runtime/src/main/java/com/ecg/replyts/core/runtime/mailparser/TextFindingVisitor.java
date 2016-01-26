package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Entity;

import java.util.ArrayList;
import java.util.List;

class TextFindingVisitor extends AbstractTextFindingVisitor {

    private final boolean excludeAttachments;

    private final List<TypedContent<String>> contents = new ArrayList<TypedContent<String>>();
    private final boolean mutable;

    public TextFindingVisitor(boolean withAttachments, boolean mutable) {
        this.mutable = mutable;
        this.excludeAttachments = !withAttachments;
    }

    @Override
    void handle(Entity e, String text, MediaType m) {
        contents.add(new StringTypedContentMime4J(m, text, mutable ? e : null));
    }

    @Override
    boolean accept(Entity e) {
        if (excludeAttachments && Mail.DISPOSITION_ATTACHMENT.equalsIgnoreCase(e.getDispositionType())) {
            return false;
        }
        return super.accept(e);

    }

    public List<TypedContent<String>> getContents() {
        return contents;
    }
}
