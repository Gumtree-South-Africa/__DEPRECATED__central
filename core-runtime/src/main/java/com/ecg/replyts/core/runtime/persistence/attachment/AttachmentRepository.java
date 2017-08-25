package com.ecg.replyts.core.runtime.persistence.attachment;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

// Interface to Swift repository for reading attachments and Kafka repository for writing them
public class AttachmentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentRepository.class);

    private final Counter MAIL_WITH_ATTACHMENTS_COUNTER = TimingReports.newCounter("attachments.mail-with-attachment-counter");
    private final Counter MAIL_COUNTER = TimingReports.newCounter("attachments.mail-counter");
    private final Histogram ATT_PER_MAIL_HISTOGRAM = TimingReports.newHistogram("attachments.number-of-attachments-per-message");
    private final Histogram MAIL_SIZE_HISTOGRAM = TimingReports.newHistogram("attachments.total-size");
    private final Histogram ATTACHMENT_SIZE_HISTOGRAM = TimingReports.newHistogram("attachments.attachment.size");
    private final Timer STORE_MAIL = TimingReports.newTimer("attachments.store-mail-timer");
    private final Timer STORE_ATTACHMENT = TimingReports.newTimer("attachments.store-attachment-timer");

    @Autowired
    private AttachmentKafkaSinkService attachmentKafkaSinkService;

    @Autowired
    private SwiftAttachmentRepository swiftAttachmentRepository;

    public Mail hasAttachments(String messageId, byte[] mailData) {

        Mail parsedMail;
        List<String> attNames;

        if (mailData.length <= 0) {
            LOG.info("No data in message '{}'", messageId);
            return null;
        }

        try {
            parsedMail = Mails.readMail(mailData);
            attNames = parsedMail.getAttachmentNames();
        } catch (ParsingException e) {
            LOG.error("Could not parse message id {}, fail to save its attachments", messageId);
            return null;
        }

        MAIL_COUNTER.inc();
        if (attNames.isEmpty()) {
            LOG.info("Message id {} has no attachments ", messageId);
            return null;
        }

        return parsedMail;
    }

    public String getCompositeKey(String messageId, String attachmentName) {
        String container = swiftAttachmentRepository.getContainer(messageId);
        return container + "/" + messageId + "/" + attachmentName;
    }

    public void storeAttachments(String messageId, Mail mail) {
        MAIL_WITH_ATTACHMENTS_COUNTER.inc();

        try (Timer.Context time = STORE_MAIL.time()) {

            long totalSize = 0;
            LOG.debug("Storing attachments {}", mail.getAttachmentNames());
            for (String aname : mail.getAttachmentNames()) {

                TypedContent<byte[]> attachment = mail.getAttachment(aname);
                if (attachment.getContent().length > 0) {

                    storeAttachment(messageId, aname, attachment);
                    totalSize +=attachment.getContent().length;
                } else {
                    LOG.debug("Empty attachment name {} for messageid {}", aname, messageId);
                }
            }

            MAIL_SIZE_HISTOGRAM.update(totalSize);
        }
        ATT_PER_MAIL_HISTOGRAM.update(mail.getAttachmentNames().size());
    }

    public void storeAttachment(String messageId, String aname, TypedContent<byte[]> attachment) {
        try (Timer.Context ignored = STORE_ATTACHMENT.time()) {
            LOG.debug("Storing message {}, attachment {} , size {} bytes", messageId, aname, attachment.getContent().length);
            attachmentKafkaSinkService.store(getCompositeKey(messageId, aname), attachment);
        }

        ATTACHMENT_SIZE_HISTOGRAM.update(attachment.getContent().length);

    }

}
