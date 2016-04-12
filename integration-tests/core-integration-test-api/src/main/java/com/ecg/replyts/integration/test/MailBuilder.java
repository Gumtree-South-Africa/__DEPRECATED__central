package com.ecg.replyts.integration.test;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Builder class that can dynamically generate a new mail format and is able to put it into the RTS dropfolder.
 */
public final class MailBuilder {

    public static final String UNIQUE_IDENTIFIER_HEADER = "X-Rts-Integration-Test-Uid";

    private final Map<String, String> headers = new LinkedHashMap<>();

    private final Map<String, byte[]> attachments = new LinkedHashMap<>();
    // mail body --> content type, as content type is not unique
    private final Map<String, String> parts = new LinkedHashMap<>();

    private MailBuilder() {

    }

    /**
     * Sets the From header
     */
    public MailBuilder from(String from) {
        return header("From", from);
    }

    /**
     * sets the receiver (to and Delivered-To)
     */
    public MailBuilder to(String to) {
        return header("To", to).header("Delivered-To", to);
    }

    /**
     * sets a a generated unique value to a header identified by <code>UNIQUE_IDENTIFIER_HEADER</code>
     */
    public MailBuilder uniqueIdentifier(String identifier) {
        return header(UNIQUE_IDENTIFIER_HEADER, identifier);
    }

    /**
     * sets the subject header
     */
    public MailBuilder subject(String s) {
        return header("Subject", s);

    }

    /**
     * sets the adid header
     */
    public MailBuilder adId(String adId) {
        return header("X-ADID", adId);
    }

    public MailBuilder randomAdId() {
        return adId(String.valueOf(Math.random()));
    }

    /**
     * Sets a custom header. Prepents X-CUST- to the header and ensures the custom header name is uppercased
     */
    public MailBuilder customHeader(String customHeader, String value) {
        return header("X-CUST-" + customHeader.toUpperCase(), value);
    }

    public void write(OutputStream o) {
        try {
            MimeMessage msg = build();
            msg.writeTo(o);
            o.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public MimeMessage build() {

        try {
            MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
            for (Map.Entry<String, String> header : headers.entrySet()) {
                msg.setHeader(header.getKey(), header.getValue());
            }
            Multipart m = new MimeMultipart();
            for (Map.Entry<String, String> part : parts.entrySet()) {
                String mime = part.getValue();
                String content = part.getKey();
                MimeBodyPart mp = new MimeBodyPart();

                mp.setText(content);
                mp.setHeader("Content-Type", mime);
                m.addBodyPart(mp);
            }
            for (Map.Entry<String, byte[]> item : attachments.entrySet()) {

                MimeBodyPart attachment = new MimeBodyPart(new InternetHeaders(), item.getValue());
                attachment.setFileName(item.getKey());
                m.addBodyPart(attachment);
            }

            msg.setContent(m);


            return msg;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Sets an arbitary header to the mail.
     */
    public MailBuilder header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Adds a(nother) body part to the mail.
     */
    public MailBuilder plainBody(String content) {
        parts.put(content, "text/plain");
        return this;
    }

    public MailBuilder htmlBody(String content) {
        parts.put(content, "text/html");
        return this;
    }

    public static MailBuilder aNewMail() {
        return new MailBuilder();
    }

    public MailBuilder randomSender() {

        return from("rnd-sender-"+Math.random()+"@host.com");
    }

    public MailBuilder randomReceiver() {

        return to("rnd-receiver-" + Math.random() + "@host.com");
    }

    public MailBuilder attachment(String s, byte[] bytes) {
        attachments.put(s, bytes);
        return this;
    }
}
