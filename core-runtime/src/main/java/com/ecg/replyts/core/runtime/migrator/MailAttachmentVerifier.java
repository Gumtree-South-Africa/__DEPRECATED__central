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
import com.google.common.collect.Sets;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MailAttachmentVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(MailAttachmentVerifier.class);

    private final Counter MAIL_COUNTER = TimingReports.newCounter("attachments.verification.mail-counter-total");
    private final Counter ATTACHMENT_COUNTER = TimingReports.newCounter("attachments.verification.attachment-counter");
    private final Counter CUM_DIFFERENCE_COUNTER = TimingReports.newCounter("attachments.verification.different");
    private final Timer BATCH_VERIFICATION_TIMER = TimingReports.newTimer("attachments.verification.batch-mail-timer");

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private DiffingRiakMailRepository mailRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private final int idBatchSize;
    private final int completionTimeoutSec;
    private final WebTarget attachmentEndpointClient;

    public MailAttachmentVerifier(int idBatchSize, int completionTimeoutSec,
                                  String attachmentEndpoint) {
        this.idBatchSize = idBatchSize;
        this.completionTimeoutSec = completionTimeoutSec;
        URI attachmentsUri = URI.create(attachmentEndpoint.startsWith("http:") ? attachmentEndpoint
                : "http://" + attachmentEndpoint);
        this.attachmentEndpointClient = ClientBuilder.newClient().target(attachmentsUri);
    }

    public void verifyIds(LocalDateTime dateFrom, LocalDateTime dateTo) {
        LOG.info("Verification of IDs has started between dates: {} - {}", dateFrom, dateTo);

        Set<String> ids1 = allIdsBetween(dateFrom, dateTo);
        LOG.info("FIRST IDs extraction has been finished. Number of IDs: {}", ids1.size());

        Set<String> ids2 = allIdsBetween(dateFrom, dateTo);
        LOG.info("SECOND IDs extraction has been finished. Number of IDs: {}", ids2.size());

        Set<String> differences = Sets.symmetricDifference(ids1, ids2);
        if (differences.isEmpty()) {
            LOG.info("There are no differences between FIRST and SECOND extractions.");
        } else {
            LOG.info("The are found differences: SIZES [{}, {}], DIFFERENCES {}", ids1.size(), ids2.size(), differences);
        }
    }

    private Set<String> allIdsBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        AtomicInteger processedBatchCounter = new AtomicInteger();

        Stream<String> mailIdStream = mailRepository.streamMailIdsCreatedBetween(
                dateFrom.toDateTime(DateTimeZone.UTC), dateTo.toDateTime(DateTimeZone.UTC));

        List<Future> results = new ArrayList<>();
        Set<String> ids = Sets.newConcurrentHashSet();
        Iterators.partition(mailIdStream.iterator(), idBatchSize)
                .forEachRemaining(mailIdBatch -> results.add(executor.submit(() -> ids.addAll(mailIdBatch))));

        Util.waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
        return ids;
    }


    public void verifyAttachmentsBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo, boolean allowMigration) {
        Stopwatch watch = Stopwatch.createStarted();

        try {
            List<Future> results = new ArrayList<>();
            AtomicInteger processedBatchCounter = new AtomicInteger();
            String verificationId = UUID.randomUUID().toString();
            Counter diffCounter = new Counter();

            LOG.info("[{}] Invoke attachment verifier from {} to {} using batch size {}", verificationId, dateFrom, dateTo, idBatchSize);

            Stream<String> mailIdStream = mailRepository.streamMailIdsCreatedBetween(
                    dateFrom.toDateTime(DateTimeZone.UTC), dateTo.toDateTime(DateTimeZone.UTC));

            Iterators.partition(mailIdStream.iterator(), idBatchSize).forEachRemaining(mailIdBatch -> {
                results.add(executor.submit(() -> verifyAttachments(verificationId, mailIdBatch, allowMigration, diffCounter)));
            });

            Util.waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
            LOG.info("[{}] Attachment verifier from {} to {} date is COMPLETED, out of total {} emails, in {} batches, total attachments {}, differences {}, cumulative-differences {}, took {}s",
                    verificationId, dateFrom, dateTo, MAIL_COUNTER.getCount(), processedBatchCounter.get(), ATTACHMENT_COUNTER.getCount(),
                    diffCounter.getCount(), CUM_DIFFERENCE_COUNTER.getCount(), watch.elapsed(TimeUnit.SECONDS));
        } finally {
            watch.stop();
        }
    }

    public void verifyAttachmentsByIds(List<String> mailIds, boolean allowMigration) {
        Stopwatch watch = Stopwatch.createStarted();

        try {
            List<Future> results = new ArrayList<>();
            AtomicInteger processedBatchCounter = new AtomicInteger();
            String verificationId = UUID.randomUUID().toString();
            Counter diffCounter = new Counter();

            LOG.info("[{}] Invoke attachment verifier for IDs {} using batch size {}", verificationId, mailIds, idBatchSize);

            Iterators.partition(mailIds.iterator(), idBatchSize).forEachRemaining(mailIdBatch -> {
                results.add(executor.submit(() -> verifyAttachments(verificationId, mailIdBatch, allowMigration, diffCounter)));
            });

            Util.waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
            LOG.info("[{}] Attachment verifier is COMPLETED, out of total {} emails, in {} batches, total attachments {}, differences {}, cumulative-differences {}, took {}s",
                    verificationId, MAIL_COUNTER.getCount(), processedBatchCounter.get(), ATTACHMENT_COUNTER.getCount(),
                    diffCounter.getCount(), CUM_DIFFERENCE_COUNTER.getCount(), watch.elapsed(TimeUnit.SECONDS));
        } finally {
            watch.stop();
        }
    }

    private void verifyAttachments(String verificationId, List<String> mailIds, boolean allowMigration, Counter diffCounter) {
        try (Timer.Context ignored = BATCH_VERIFICATION_TIMER.time()) {

            for (String messageId : mailIds) {

                if (StringUtils.isEmpty(messageId)) {
                    continue;
                }
                LOG.debug("[{}] Next messageId is {} ", verificationId, messageId);

                if (messageId.trim().contains("@Part0")) {
                    messageId = messageId.substring(0, messageId.indexOf("@Part0"));
                    LOG.debug("Stripping @Part0, next message id is {} ", messageId);
                }

                if (messageId.trim().contains("@Part")) {
                    continue;
                }

                byte[] rawmail = loadMail(verificationId, true, messageId);

                if (rawmail == null) {
                    LOG.info("[{}] Did not find raw mail for message id in incoming messages {}", verificationId, messageId);
                    rawmail = loadMail(verificationId, false, messageId);

                    if (rawmail == null) {
                        LOG.info("[{}] Did not find raw mail for message id {}", verificationId, messageId);
                        return;
                    }
                }
                MAIL_COUNTER.inc();

                Mail parsedMail = attachmentRepository.hasAttachments(messageId, rawmail);
                if (parsedMail == null) {
                    LOG.debug("[{}] Message {} does not have any attachments", verificationId, messageId);
                    return;
                }

                compareAttachments(verificationId, parsedMail, messageId, allowMigration, diffCounter);
            }
        }
    }

    private void compareAttachments(String verificationId, Mail mail, String messageId, boolean allowMigration, Counter diffCounter) {
        for (String filename : mail.getAttachmentNames()) {

            TypedContent<byte[]> riakAttachment = mail.getAttachment(filename);
            ATTACHMENT_COUNTER.inc();

            try {
                Response clientResponse = invokeAttachmentController(verificationId, messageId, filename);

                try {
                    if (clientResponse.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                        byte[] swiftAttachment = clientResponse.readEntity(byte[].class);

                        if (Arrays.equals(swiftAttachment, riakAttachment.getContent())) {
                            LOG.info("[{}] An attachment successfully verified. Message ID: {}, Filename: {}", verificationId, messageId, filename);
                        } else {
                            LOG.error("[{}] Failed to compare an attachment, the attachment from new API is different then the attachment " +
                                    "from RIAK database. Message ID: {}, Filename: {}", verificationId, messageId, filename);
                            // never happened so far
                            CUM_DIFFERENCE_COUNTER.inc();
                            diffCounter.inc();
                        }
                    } else {
                        LOG.error("[{}] Failed to retrieve an attachment using new API. Status Code: {}, Message ID: {}, Filename: {}",
                                verificationId, clientResponse.getStatus(), messageId, filename);
                        CUM_DIFFERENCE_COUNTER.inc();
                        diffCounter.inc();

                        if (allowMigration) {
                            LOG.info("[{}] Attachment is automatically migrated. Message ID: {}, Filename: {}", verificationId, messageId, filename);
                            attachmentRepository.storeAttachment(messageId, filename, riakAttachment);
                        }
                    }
                } finally {
                    try {
                        clientResponse.close();
                    } catch (Exception e) {
                        LOG.error("an error occurred while closing the reponse: ", e);
                    }
                }
            } catch (Exception ex) {
                String error = String.format("[%s] Client invocation failed to retrieve an attachment using new API. Message ID: %s, Filename: %s",
                        verificationId, messageId, filename);
                LOG.error(error, ex);
                CUM_DIFFERENCE_COUNTER.inc();
                diffCounter.inc();
                if (allowMigration) {
                    LOG.info("[{}] Comparison failed migrating attachment. Message ID: {}, Filename: {}", verificationId, messageId, filename);
                    attachmentRepository.storeAttachment(messageId, filename, riakAttachment);
                }
            }
        }
    }

    private Response invokeAttachmentController(String verificationId, String messageId, String filename) throws Exception {
        WebTarget target = attachmentEndpointClient.path("/screeningv2/mail/{messageId}/attachments/{filename}")
                .resolveTemplate("messageId", messageId)
                .resolveTemplate("filename", filename);

        LOG.debug("[{}] Invoking AttachmentController, Message ID: {}, Filename: {}, URL: {}", verificationId, messageId, filename, target.getUri());
        return target.request().get();
    }

    private byte[] loadMail(String verificationId, boolean inbound, String messageId) {
        try {
            return inbound ? mailRepository.readInboundMail(messageId) : mailRepository.readOutboundMail(messageId);
        } catch (Exception ex) {
            LOG.error("[{}] Failed to load mail for message ID '{}' ", verificationId, messageId, ex);
            return null;
        }
    }
}
