package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageFixer;
import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

class StructuredMutableMail implements MutableMail {
    private final StructuredMail mail;

    StructuredMutableMail(StructuredMail mail) {
        this.mail = mail;
    }

    @Override
    public void addHeader(String name, String value) {
        mail.getMailHeader().addHeader(name, value);
    }

    @Override
    public void removeHeader(String name) {
        mail.getMailHeader().removeHeader(name);
    }

    @Override
    public void setFrom(MailAddress newFrom) {
        mail.getMailHeader().setFrom(newFrom.getAddress());
    }

    @Override
    public void setTo(MailAddress newTo) {
        mail.getMailHeader().setTo(newTo.getAddress());
    }

    @Override
    public void setReplyTo(MailAddress newReplyTo) {
        mail.getMailHeader().setReplyTo(newReplyTo.getAddress());
    }

    @Override
    public void applyOutgoingMailFixes(List<MessageFixer> fixers, Exception originalException) {
        if (fixers == null) {
            return;
        }

        Message originalMessage = mail.getOriginalMessage();
        for (MessageFixer fixer : fixers) {
            fixer.applyIfNecessary(originalMessage, originalException);
        }
    }

    @Override
    public Map<String, List<String>> getDecodedHeaders() {
        return mail.getDecodedHeaders();
    }

    @Override
    public Map<String, String> getUniqueHeaders() {
        return mail.getUniqueHeaders();
    }

    @Override
    public List<String> getHeaders(String name) {
        return mail.getHeaders(name);
    }

    @Override
    public String getUniqueHeader(String headerName) {
        return mail.getUniqueHeader(headerName);
    }

    @Override
    public boolean containsHeader(String string) {
        return mail.containsHeader(string);
    }

    @Override
    public String getFrom() {
        return mail.getFrom();
    }

    @Override
    public String getReplyTo() {
        return mail.getReplyTo();
    }

    @Override
    public String getFromName() {
        return mail.getFromName();
    }

    @Override
    public String getDeliveredTo() {
        return mail.getDeliveredTo();
    }

    @Override
    public String getSubject() {
        return mail.getSubject();
    }

    @Override
    public Date getSentDate() {
        return mail.getSentDate();
    }

    @Override
    public List<String> getTo() {
        return mail.getTo();
    }

    @Override
    public String getMessageId() {
        return mail.getMessageId();
    }

    @Override
    public Optional<String> getLastReferencedMessageId() {
        return mail.getLastReferencedMessageId();
    }

    @Override
    public MediaType getMainContentType() {
        return mail.getMainContentType();
    }

    @Override
    public String getAdId() {
        return mail.getAdId();
    }

    @Override
    public Map<String, String> getCustomHeaders() {
        return mail.getCustomHeaders();
    }

    @Override
    public boolean isMultiPart() {
        return mail.isMultiPart();
    }

    @Override
    public boolean hasAttachments() {
        return mail.hasAttachments();
    }

    @Override
    public List<String> getAttachmentNames() {
        return mail.getAttachmentNames();
    }

    @Override
    public MutableMail makeMutableCopy() {
        throw new UnsupportedOperationException("can't make a mutable copy of a mutable mail - just use this object");
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        mail.writeTo(outputStream);
    }

    @Override
    public List<TypedContent<String>> getTextParts(boolean includeAttachments) {
        return mail.getMailBodyVisitingClient().visit(new TextFindingVisitor(includeAttachments, true)).getContents();
    }

    @Override
    public List<String> getPlaintextParts() {
        return mail.getPlaintextParts();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public TypedContent<byte[]> getAttachment(String filename) {
        return mail.getAttachment(filename);
    }

    @Override
    public String toString() {
        return "StructuredMutableMail{" +
                "mail=" + mail +
                '}';
    }
}
