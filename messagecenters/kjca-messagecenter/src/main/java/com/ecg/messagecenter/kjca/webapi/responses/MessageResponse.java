package com.ecg.messagecenter.kjca.webapi.responses;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

public class MessageResponse {

    private static final Pattern REMOVE_DOUBLE_WHITESPACES = Pattern.compile("\\s+");

    private final String messageId;
    private final String receivedDate;
    private final MailTypeRts boundness;
    private final String textShort;
    private final String textShortTrimmed;
    private final Optional<String> phoneNumber;
    private final List<Attachment> attachments;
    private final String senderEmail;

    public MessageResponse(String receivedDate, MailTypeRts boundness, String textShort, Optional<String> phoneNumber, List<Attachment> attachments, String senderEmail) {
        this(null, receivedDate, boundness, textShort, phoneNumber, attachments, senderEmail);
    }

    public MessageResponse(String messageId, String receivedDate, MailTypeRts boundness, String textShort, Optional<String> phoneNumber, List<Attachment> attachments, String senderEmail) {
        this.messageId = messageId;
        this.receivedDate = receivedDate;
        this.boundness = boundness;
        this.textShort = textShort;
        this.phoneNumber = phoneNumber;
        this.attachments = attachments;
        this.senderEmail = senderEmail;
        this.textShortTrimmed = REMOVE_DOUBLE_WHITESPACES.matcher(textShort).replaceAll(" ");
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public MailTypeRts getBoundness() {
        return boundness;
    }

    public String getTextShortTrimmed() {
        return textShortTrimmed;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public String getTextShort() {
        StringBuilder b = new StringBuilder();
        b.append(textShort);
        if (phoneNumber.isPresent()) {
            b.append("\n\nTel.: ").append(phoneNumber.get());
        }
        return b.toString();
    }

    public static class Attachment {

        private final String filename;
        private final String url;

        public Attachment(String filename, String messageId) {
            this.filename = filename;
            this.url = String.format("/screeningv2/mail/%s/INBOUND/parts/%s",
                    messageId,
                    urlPathSegmentEscaper().escape(filename)
            );
        }

        public String getTitle() {
            return filename;
        }

        public String getFormat() {
            List<String> items = Splitter.on('.').splitToList(filename);
            String mime = items.get(items.size()-1).toLowerCase();
            switch (mime) {
                case "jpg": return MediaType.JPEG.toString();
                case "gif": return MediaType.GIF.toString();
                case "png": return MediaType.PNG.toString();
                case "bmp": return MediaType.BMP.toString();
                case "tif": return MediaType.TIFF.toString();
                case "pdf": return MediaType.PDF.toString();
                case "doc": return MediaType.MICROSOFT_WORD.toString();
                case "docx": return MediaType.MICROSOFT_WORD.toString();
                case "zip": return MediaType.ZIP.toString();
                default: return MediaType.APPLICATION_BINARY.toString();
            }
        }

        public String getUrl() {
            return url;
        }

        public static List<Attachment> transform(Message firstMessage) {
            List<MessageResponse.Attachment> attachments = Lists.newArrayList();
            for (String filename : firstMessage.getAttachmentFilenames()) {
                attachments.add(new MessageResponse.Attachment(filename, firstMessage.getId()));
            }
            return ImmutableList.copyOf(attachments);
        }
    }

}
