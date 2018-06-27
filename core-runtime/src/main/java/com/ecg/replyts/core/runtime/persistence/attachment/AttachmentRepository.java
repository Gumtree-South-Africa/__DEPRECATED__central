package com.ecg.replyts.core.runtime.persistence.attachment;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.Attachment;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
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
    @Qualifier("attachmentSink")
    private KafkaSinkService attachmentKafkaSinkService;

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
            LOG.trace("Message id {} has no attachments ", messageId);
            return null;
        }

        return parsedMail;
    }

    public String getCompositeKey(String messageId, String attachmentName) {
        String container = swiftAttachmentRepository.getContainer(messageId);
        return container + "/" + messageId + "/" + attachmentName;
    }

    public void storeAttachments(String messageId, Collection<Attachment> attachments) {
        MAIL_WITH_ATTACHMENTS_COUNTER.inc();

        try (Timer.Context ignored = STORE_MAIL.time()) {
            long totalSize = 0;
            LOG.trace("Storing attachments {}", attachments);
            for (Attachment attachment : attachments) {
                if (attachment.getPayload().length > 0) {
                    storeAttachment(messageId, attachment.getName(), attachment.getPayload());
                    totalSize += attachment.getPayload().length;
                } else {
                    LOG.debug("Empty attachment name {} for messageid {}", attachment.getName(), messageId);
                }
            }

            MAIL_SIZE_HISTOGRAM.update(totalSize);
        }
        ATT_PER_MAIL_HISTOGRAM.update(attachments.size());
    }

    private void storeAttachment(String messageId, String aname, byte[] payload) {
        try (Timer.Context ignored = STORE_ATTACHMENT.time()) {
            String key = getCompositeKey(messageId, aname);
            LOG.debug("Storing message {}, attachment {}, size {} bytes, as {}", messageId, aname, payload.length, key);
            attachmentKafkaSinkService.store(key, payload);
        }

        ATTACHMENT_SIZE_HISTOGRAM.update(payload.length);
    }

}
