package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.IndexingMode;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;
import com.hazelcast.core.HazelcastInstance;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.migrator.Util.waitForCompletion;

public class MailAttachmentMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(MailAttachmentMigrator.class);

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private DiffingRiakMailRepository mailRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private HazelcastInstance hazelcast;

    private int idBatchSize;
    private int conversationMaxAgeDays;
    private int completionTimeoutSec;

    private AtomicInteger processedBatchCounter = new AtomicInteger();
    private AtomicLong submittedBatchCounter = new AtomicLong();
    private final Counter MAIL_COUNTER = TimingReports.newCounter("migration.attachments.mail-counter");
    private final Timer BATCH_MIGRATION_TIMER = TimingReports.newTimer("migration.attachments.batch-mail-timer");

    private Stopwatch watch;

    public MailAttachmentMigrator(int idBatchSize, int conversationMaxAgeDays, int completionTimeoutSec) {
        this.idBatchSize = idBatchSize;
        this.conversationMaxAgeDays = conversationMaxAgeDays;
        this.completionTimeoutSec = completionTimeoutSec;
    }

    public String getRateMailsPerSec() {
        if (processedBatchCounter.get() > 0) {
            return String.format("%.2f ", (processedBatchCounter.get() * idBatchSize / (double) watch.elapsed(TimeUnit.SECONDS)));
        }
        return "";
    }

    public long getBatchesMigrated() {
        return processedBatchCounter.get();
    }

    public long getTimeTaken(TimeUnit tunit) {
        return watch.elapsed(tunit);
    }

    public boolean migrateAll() {
        LOG.info("migrateAll started at {}", new LocalDateTime());
        return migrateFromDate(getStartingTime());
    }

    public boolean migrateById(List<String> mailIds) {
        String msg = String.format(" migrate by ids %s ", mailIds);
        watch = Stopwatch.createStarted();
        return execute(() -> migrateAttachments(mailIds), msg);
    }

    public boolean migrateFromDate(LocalDateTime dateFrom) {
        String msg = String.format(" migrateFromDate %s ", dateFrom);
        return execute(() -> migrateBetweenDates(dateFrom, new LocalDateTime()), msg);
    }

    public boolean migrate(LocalDateTime dateFrom, LocalDateTime dateTo) {
        String msg = String.format(" migrate between %s and %s ", dateFrom, dateTo);
        return execute(() -> migrateBetweenDates(dateFrom, dateTo), msg);
    }

    boolean execute(Runnable runnable, String msg) {
        return Util.execute(hazelcast, executor, runnable, msg, LOG);
    }

    private LocalDateTime getStartingTime() {
        return new LocalDateTime(new Date()).minusDays(conversationMaxAgeDays);
    }

    public void migrateBetweenDates(LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            List<Future> results = new ArrayList<>();
            processedBatchCounter.set(0);
            submittedBatchCounter.set(0);
            watch = Stopwatch.createStarted();
            LOG.info("Migrating attachments in mails using batch size {}", idBatchSize);

            Stream<String> mailIdStream = mailRepository.streamMailIdsCreatedBetween(dateFrom.toDateTime(DateTimeZone.UTC),
                    dateTo.toDateTime(DateTimeZone.UTC));

            Iterators.partition(mailIdStream.iterator(), idBatchSize).forEachRemaining(mailIdBatch -> {
                results.add(executor.submit(() -> {
                    migrateAttachments(mailIdBatch);
                }));
            });

            waitForCompletion(results, processedBatchCounter, LOG, completionTimeoutSec);
            LOG.info("Attachment migration from {} to {} date is COMPLETED,  {} mails with attachment migrated, in {} batches, collected batches {}, took {}s",
                    dateFrom, dateTo, MAIL_COUNTER.getCount(), BATCH_MIGRATION_TIMER.getCount(), processedBatchCounter.get(), watch.elapsed(TimeUnit.SECONDS));
        } finally {
            watch.stop();
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
    }

    private void migrateAttachments(List<String> mailIds) {
        try (Timer.Context ignored = BATCH_MIGRATION_TIMER.time()) {

            for (String messageId : mailIds) {
                byte[] rawmail = loadMail(true, messageId);

                if (rawmail == null) {
                    LOG.info("Did not find raw mail for message id in incoming messages {}", messageId);
                    rawmail = loadMail(false, messageId);

                    if (rawmail == null) {
                        LOG.info("Did not find raw mail for message id {}", messageId);
                        return;
                    }
                }

                Mail parsedMail = attachmentRepository.hasAttachments(messageId, rawmail);
                if (parsedMail == null) {
                    LOG.debug("Message {} does not have any attachments", messageId);
                    return;
                }
                // No need to acquire a global lock because messageIds are unique
                attachmentRepository.storeAttachments(messageId, parsedMail);

                MAIL_COUNTER.inc();
            }
        }
    }

    private byte[] loadMail(boolean inbound, String messageId) {
        byte[] rawmail = null;
        try {
            if (inbound) {
                rawmail = mailRepository.readInboundMail(messageId);
            } else {
                rawmail = mailRepository.readOutboundMail(messageId);
            }
        } catch (Exception ex) {
            String from = inbound ? "inbound" : "outbound";
            LOG.error("Failed to load {} mail for messageid '{}' ", from, messageId, ex);
        }

        return rawmail;
    }


}
