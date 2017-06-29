package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MailAttachmentVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(MailAttachmentVerifier.class);

    private final Counter MAIL_COUNTER = TimingReports.newCounter("attachments.verification.mail-counter-total");
    private final Timer BATCH_VERIFICATION_TIMER = TimingReports.newTimer("attachments.verification.batch-mail-timer");

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private DiffingRiakMailRepository mailRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Value("${attachments.verification.endpoint:localhost:8080}")
    private String attachmentEndpoint;

    private int idBatchSize;
    private int completionTimeoutSec;

    public MailAttachmentVerifier(int idBatchSize, int completionTimeoutSec) {
        this.idBatchSize = idBatchSize;
        this.completionTimeoutSec = completionTimeoutSec;
    }

    public void verifyAttachmentsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo, boolean allowMigration) {
        Stopwatch watch = Stopwatch.createStarted();

        try {
            List<Future> results = new ArrayList<>();
            AtomicInteger processedBatchCounter = new AtomicInteger();

            LOG.info("Verification attachments in mails using batch size {}", idBatchSize);
            Stream<String> mailIdStream = mailRepository.streamMailIdsCreatedBetween(
                    dateFrom.toDateTime(DateTimeZone.UTC), dateTo.toDateTime(DateTimeZone.UTC));

            Iterators.partition(mailIdStream.iterator(), idBatchSize).forEachRemaining(mailIdBatch -> {
                results.add(executor.submit(() -> verifyAttachments(mailIdBatch, allowMigration)));
            });

            Util.waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
            LOG.info("Attachment verifier from {} to {} date is COMPLETED, out of total {} emails, in {} batches, took {}s",
                    dateFrom, dateTo, MAIL_COUNTER.getCount(), processedBatchCounter.get(), watch.elapsed(TimeUnit.SECONDS));
        } finally {
            watch.stop();
        }
    }

    public void verifyAttachmentsByIds(List<String> mailIds, boolean allowMigration) {
        Stopwatch watch = Stopwatch.createStarted();

        try {
            List<Future> results = new ArrayList<>();
            AtomicInteger processedBatchCounter = new AtomicInteger();

            Iterators.partition(mailIds.iterator(), idBatchSize).forEachRemaining(mailIdBatch -> {
                results.add(executor.submit(() -> verifyAttachments(mailIdBatch, allowMigration)));
            });

            Util.waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
            LOG.info("Attachment verifier is COMPLETED, out of total {} emails, in {} batches, took {}s",
                    MAIL_COUNTER.getCount(), processedBatchCounter.get(), watch.elapsed(TimeUnit.SECONDS));
        } finally {
            watch.stop();
        }
    }

    private void verifyAttachments(List<String> mailIds, boolean allowMigration) {
        try (Timer.Context ignored = BATCH_VERIFICATION_TIMER.time()) {

            for (String messageId : mailIds) {

                if (StringUtils.isEmpty(messageId)) {
                    continue;
                }
                LOG.debug("Next messageId is {} ", messageId);

                if (messageId.trim().contains("@Part0")) {
                    messageId = messageId.substring(0, messageId.indexOf("@Part0"));
                    LOG.debug("Stripping @Part0, next message id is {} ", messageId);
                }

                if (messageId.trim().contains("@Part")) {
                    continue;
                }

                byte[] rawmail = loadMail(true, messageId);

                if (rawmail == null) {
                    LOG.info("Did not find raw mail for message id in incoming messages {}", messageId);
                    rawmail = loadMail(false, messageId);

                    if (rawmail == null) {
                        LOG.info("Did not find raw mail for message id {}", messageId);
                        return;
                    }
                }
                MAIL_COUNTER.inc();

                Mail parsedMail = attachmentRepository.hasAttachments(messageId, rawmail);
                if (parsedMail == null) {
                    LOG.debug("Message {} does not have any attachments", messageId);
                    return;
                }

                compareAttachments(parsedMail, messageId, allowMigration);
            }
        }
    }

    private void compareAttachments(Mail mail, String messageId, boolean allowMigration) {
        for (String filename : mail.getAttachmentNames()) {
            TypedContent<byte[]> riakAttachment = mail.getAttachment(filename);

            try {
                ClientResponse<byte[]> clientResponse = invokeAttachmentController(messageId, filename);

                if (clientResponse.getResponseStatus().getFamily() == Response.Status.Family.SUCCESSFUL) {
                    byte[] swiftAttachment = clientResponse.getEntity();

                    if (Arrays.equals(swiftAttachment, riakAttachment.getContent())) {
                        LOG.info("An attachment successfully verified. Message ID: {}, Filename: {}", messageId, filename);
                    } else {
                        LOG.error("Failed to compare an attachment, the attachment from new API is different then the attachment " +
                                "from RIAK database. Message ID: {}, Filename: {}", messageId, filename);
                    }
                } else {
                    LOG.error("Failed to retrieve an attachment using new API. Status Code: {}, Message ID: {}, Filename: {}",
                            clientResponse.getStatus(), messageId, filename);

                    if (allowMigration) {
                        LOG.info("Attachment is automatically migrated. Message ID: {}, Filename: {}", messageId);
                        attachmentRepository.storeAttachment(messageId, filename, riakAttachment);
                    }
                }
            } catch (Exception ex) {
                String error = String.format("Client invocation failed to retrieve an attachment using new API. Message ID: %s, Filename: %s",
                        messageId, filename);
                LOG.error(error, ex);
            }
        }
    }

    private ClientResponse<byte[]> invokeAttachmentController(String messageId, String filename) throws Exception {
        String uri = UriBuilder.fromPath("http://{attachmentEndpoint}/screeningv2/mail/{messageId}/attachments/{filename}")
                .build(attachmentEndpoint, messageId, filename)
                .toString();

        LOG.debug("Invoking AttachmentController, Message ID: {}, Filename: {}, URL: {}", messageId, filename, uri);
        return new ClientRequest(uri).get(byte[].class);
    }

    private byte[] loadMail(boolean inbound, String messageId) {
        try {
            return inbound ? mailRepository.readInboundMail(messageId) : mailRepository.readOutboundMail(messageId);
        } catch (Exception ex) {
            LOG.error(String.format("Failed to load mail for message ID '%s' ", messageId), ex);
            return null;
        }
    }
}
