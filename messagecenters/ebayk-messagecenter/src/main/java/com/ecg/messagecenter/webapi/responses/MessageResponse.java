package com.ecg.messagecenter.webapi.responses;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

/**
 * User: maldana
 * Date: 30.10.13
 * Time: 17:16
 *
 * @author maldana@ebay.de
 */
public class MessageResponse {

    private static final Pattern REMOVE_DOUBLE_WHITESPACES = Pattern.compile("\\s+");

    private final String messageId;
    private final String receivedDate;
    private final MailTypeRts boundness;
    private final String textShort;
    private final String textShortTrimmed;
    private final Optional<String> phoneNumber;
    private final List<Attachment> attachments;
    private final String offerId;
    private final String senderEmail;

    public MessageResponse(@Nullable String messageId, String receivedDate, String offerId, MailTypeRts boundness,
                           String textShort, Optional<String> phoneNumber, List<Attachment> attachments) {
        this(messageId, receivedDate, offerId, boundness, textShort, phoneNumber, attachments, null);
    }

    public MessageResponse(@Nullable String messageId, String receivedDate, String offerId, MailTypeRts boundness,
                           String textShort, Optional<String> phoneNumber, List<Attachment> attachments,
                           String senderEmail) {
        this.messageId = messageId;
        this.receivedDate = receivedDate;
        this.boundness = boundness;
        this.textShort = textShort;
        this.phoneNumber = phoneNumber;
        this.attachments = attachments;
        this.senderEmail = senderEmail;
        this.offerId = offerId;
        this.textShortTrimmed = REMOVE_DOUBLE_WHITESPACES.matcher(textShort).replaceAll(" ");
    }

    @Nullable
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

    public Long getOfferId() {
        return offerId == null ? null : Long.valueOf(offerId);
    }

    public String getTextShort() {
        StringBuilder b = new StringBuilder();
        b.append(textShort);
        phoneNumber.ifPresent(s -> b.append("\n\nTel.: ").append(s));
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

        public static List<Attachment> transform(Message message) {
            List<MessageResponse.Attachment> attachments = Lists.newArrayList();
            for (String filename : message.getAttachmentFilenames()) {
                // we use custom technical .xml attachments for openimmo integration during contact-poster
                // therefore exclude them
                if (!filename.endsWith(".xml")) {
                    attachments.add(new MessageResponse.Attachment(filename, message.getId()));
                }
            }
            return ImmutableList.copyOf(attachments);
        }
    }

}
