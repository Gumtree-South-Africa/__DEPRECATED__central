package com.ecg.messagecenter.webapi.responses;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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

    private final String receivedDate;
    private final MailTypeRts boundness;
    private final String textShort;
    private final String textShortTrimmed;
    private final Optional<String> phoneNumber;
    private final List<Attachment> attachments;
    private final Optional<String> offerId;
    private final Optional<String> robot;
    private final String senderEmail;
    private final List<MessageLink> messageLinks;
    private final Optional<RobotMessageResponse> robotResponse;

    public MessageResponse(String receivedDate, Optional<String> offerId, Optional<String> robot, MailTypeRts boundness,
                           String textShort, Optional<String> phoneNumber, List<Attachment> attachments, List<MessageLink> messageLinks,
                           Optional<RobotMessageResponse> robotResponse) {
        this(receivedDate, offerId, robot, boundness, textShort, phoneNumber, attachments, null, messageLinks, robotResponse);
    }

    public MessageResponse(String receivedDate, Optional<String> offerId, Optional<String> robot, MailTypeRts boundness,
                           String textShort, Optional<String> phoneNumber, List<Attachment> attachments, String senderEmail,
                           List<MessageLink> messageLinks, Optional<RobotMessageResponse> robotResponse) {
        this.receivedDate = receivedDate;
        this.boundness = boundness;
        this.textShort = textShort;
        this.phoneNumber = phoneNumber;
        this.attachments = attachments;
        this.senderEmail = senderEmail;
        this.offerId = offerId;
        this.robot = robot;
        this.robotResponse = robotResponse;
        this.textShortTrimmed = REMOVE_DOUBLE_WHITESPACES.matcher(textShort).replaceAll(" ");
        this.messageLinks = messageLinks;
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

    public String getOfferId() {
        return offerId.isPresent() ? offerId.get() : null;
    }

    public String getRobot() { return robot.isPresent() ? robot.get() : null; }

    public RobotMessageResponse getRobotResponse() {
        return robotResponse.orElse(null);
    }

    public String getTextShort() {
        StringBuilder b = new StringBuilder();
        b.append(textShort);
        if (phoneNumber.isPresent()) {
            b.append("\n\nTel.: ").append(phoneNumber.get());
        }
        return b.toString();
    }

    public List<MessageLink> getMessageLinks() {
        return messageLinks;
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

        public static List<Attachment> transform(List<String> attachmentFilenames, String messageId) {
            if(CollectionUtils.isEmpty(attachmentFilenames) || !StringUtils.hasText(messageId)){
                return new ArrayList<>();
            }

            List<MessageResponse.Attachment> attachments = Lists.newArrayList();
            for (String filename : attachmentFilenames) {
                attachments.add(new MessageResponse.Attachment(filename, messageId));
            }
            return ImmutableList.copyOf(attachments);
        }
    }

    public static class MessageLink {
        private final int start;
        private final int end;
        private final String type;
        private final String url;

        public MessageLink(int start, int end, String type, String url) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.url = url;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }
    }
}
