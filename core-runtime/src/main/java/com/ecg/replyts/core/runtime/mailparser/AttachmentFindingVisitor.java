package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.collect.ImmutableList;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;

import java.util.ArrayList;
import java.util.List;

class AttachmentFindingVisitor implements MailBodyVisitor {

    private List<Entity> attachments = new ArrayList<Entity>();

    @Override
    public void visit(Entity e, SingleBody body) {
        if (Mail.DISPOSITION_ATTACHMENT.equalsIgnoreCase(e.getDispositionType())) {
            attachments.add(e);
        }
    }

    public List<Entity> getAttachments() {
        return ImmutableList.copyOf(attachments);
    }

    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    public List<String> names() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (Entity e : attachments) {
            String filename = e.getFilename();
            if(filename!=null) {
                // some mail clients encode filenames in a very weird format when they contain non-ascii characters. Mime4J will then not be able to read the filename and return null instead.
                // A mail with such an attachment will be undeliverable. therefore, if the attachment does not have a filename, skip it.
                builder.add(filename);
            }
        }
        return builder.build();
    }
}
