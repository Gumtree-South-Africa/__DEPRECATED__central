package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.Mail;

import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.stream.Stream;

// Interface to Swift repository for reading attachments and Kafka repository for writing them
public class HybridMailRepository implements MailRepository {

    private static final Logger LOG = LoggerFactory.getLogger(HybridMailRepository.class);

    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private SwiftAttachmentRepository swiftAttachmentRepository;
    @Autowired
    private DiffingRiakMailRepository diffingRiakMailRepository;
    @Autowired
    private HybridMigrationClusterState hybridMigrationClusterState;

    @Override
    public void persistMail(String messageId, byte[] mailData, com.google.common.base.Optional<byte[]> ignored) {

        diffingRiakMailRepository.doPersist(messageId, mailData);

        if (mailData.length > 0) {
            LOG.info("Persisting attachments for message '{}'", messageId);
            storeAttachments(messageId, mailData);
        }
    }

    private void storeAttachments(String messageId, byte[] mailData) {
        Preconditions.checkNotNull(messageId);
        Preconditions.checkNotNull(mailData);

        Mail parsedMail = attachmentRepository.hasAttachments(messageId, mailData);

        if (parsedMail == null) {
            LOG.debug("Message {} does not have any attachments", messageId);
            return;
        }

        if (isInSwift(messageId)) {
            LOG.info("Message id {} has been already migrated ", messageId);
            return;
        }

        if (hybridMigrationClusterState.tryClaim(Mail.class, messageId)) {
            attachmentRepository.storeAttachments(messageId, parsedMail);
        } else {
            LOG.debug("Failed to acquire a lock on message {}. Assuming migration is already in progress", messageId);
        }

    }

    public void migrateAttachments(String messageId) {
        byte[] rawmail = diffingRiakMailRepository.readInboundMail(messageId);
        if (rawmail != null) {
            storeAttachments(messageId, rawmail);
        } else {
            LOG.info("Did not find raw mail for message id {}", messageId);
        }
    }

    // This is potentially expensive. We do not have to do it but then attachment traffic could be heavy
    private boolean isInSwift(String messageId) {
        return !swiftAttachmentRepository.getNames(messageId).orElse(Collections.emptyMap()).isEmpty();
    }

    @Override
    public byte[] readInboundMail(String messageId) {
        return diffingRiakMailRepository.readInboundMail(messageId);
    }

    @Override
    public Mail readInboundMailParsed(String messageId) {
        return diffingRiakMailRepository.readInboundMailParsed(messageId);
    }

    @Override
    public byte[] readOutboundMail(String messageId) {
        return diffingRiakMailRepository.readOutboundMail(messageId);
    }

    @Override
    public Mail readOutboundMailParsed(String messageId) {
        return diffingRiakMailRepository.readOutboundMailParsed(messageId);
    }

    @Override
    public void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
         diffingRiakMailRepository.deleteMailsByOlderThan(time, maxResults, numCleanUpThreads);
    }

    @Override
    public void deleteMail(String messageId) {
        diffingRiakMailRepository.deleteMail(messageId);
    }

    @Nonnull
    @Override
    public Stream<String> streamMailIdsSince(DateTime fromTime) {
        return diffingRiakMailRepository.streamMailIdsSince(fromTime);
    }

    @Nonnull
    @Override
    public Stream<String> streamMailIdsCreatedBetween(DateTime fromTime, DateTime toTime) {
        return diffingRiakMailRepository.streamMailIdsCreatedBetween(fromTime, toTime);
    }

}
