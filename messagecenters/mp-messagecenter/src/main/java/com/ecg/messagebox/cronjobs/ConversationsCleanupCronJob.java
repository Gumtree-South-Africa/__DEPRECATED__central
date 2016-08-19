package com.ecg.messagebox.cronjobs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagebox.util.InstrumentedExecutorService;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newGauge;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static org.joda.time.DateTime.now;

public class ConversationsCleanupCronJob implements CronJobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationsCleanupCronJob.class);
    private static final DateTime STARTING_DATE = new DateTime(1445774400000L); // 2015-10-25 13:00:00
    private static final String CONVERSATIONS_CLEANUP_CRONJOB_NAME = "conversations_cleanup_cronjob";

    private final Timer processHourTimer = newTimer("conversationsCleanupCronjob.processHour");
    private final Counter deleteConversationCounter = newCounter("conversationsCleanupCronjob.deleteConversation");
    private final Counter deleteModificationIndexCounter = newCounter("conversationsCleanupCronjob.deleteModificationIndexByDate");
    private final Counter deleteConversationExceptionCounter = newCounter("conversationsCleanupCronjob.deleteConversationException");
    private final Counter deleteModificationIndexExceptionCounter = newCounter("conversationsCleanupCronjob.deleteModificationIndexException");
    private final Counter failedCounter = newCounter("conversationsCleanupCronjob.failed");
    private final Counter succeededCounter = newCounter("conversationsCleanupCronjob.succeeded");

    private final int maxConversationAgeDays;
    private final int batchSize;
    private final CassandraPostBoxRepository repository;
    private final String cronJobExpression;
    private final ExecutorService executorService;

    @Autowired
    ConversationsCleanupCronJob(@Qualifier("newCassandraPostBoxRepo") CassandraPostBoxRepository repository,
                                @Value("${workQueueSize:400}") int workQueueSize,
                                @Value("${threadCount:4}") int threadCount,
                                @Value("${maxConversationAgeDays:120}") int maxConversationAgeDays,
                                @Value("${batchSize:300}") int batchSize,
                                @Value("${expression:0 0 0 * * ? *}") String cronJobExpression
    ) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        newGauge("conversationsCleanupCronjob.cronjobExecSrv.workQueueSizeGauge", () -> workQueue.size());
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("conversationsCleanupCronjob", "cronjobExecSrv");

        this.executorService = new InstrumentedExecutorService(
                new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler),
                "conversationsCleanupCronjob",
                "cronjobExecSrv"
        );
        this.repository = repository;
        this.cronJobExpression = cronJobExpression;
        this.maxConversationAgeDays = maxConversationAgeDays;
        this.batchSize = batchSize;
    }

    @Override
    public void execute() throws Exception {
        final DateTime deleteEverythingBefore = now().minusDays(maxConversationAgeDays);
        DateTime lastProcessedDate = repository.getCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME);
        DateTime dateToProcess = lastProcessedDate == null ? STARTING_DATE : lastProcessedDate.plusHours(1);

        while (dateToProcess.isBefore(deleteEverythingBefore)) {
            try (Timer.Context ignored = processHourTimer.time()) {
                Stream<ConversationModification> conversationModifications = repository.getConversationModificationsByHour(dateToProcess);
                Iterator<List<ConversationModification>> partitions = Iterators.partition(conversationModifications.iterator(), batchSize);

                LOGGER.info("{}: Deleting conversations for {} date", CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess);

                while (partitions.hasNext()) {
                    List<Future> cleanupTasks = partitions.next().stream().
                            map(conv -> cleanupConversation(deleteEverythingBefore, conv)).
                            map(task -> executorService.submit(task)).
                            collect(Collectors.toList());

                    for (Future task : cleanupTasks) {
                        try {
                            task.get(1, TimeUnit.SECONDS);
                        } catch (InterruptedException | ExecutionException e) {
                            failedCounter.inc();
                            LOGGER.error("{} failed", CONVERSATIONS_CLEANUP_CRONJOB_NAME, e);
                            return;
                        }
                    }
                }

                repository.setCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess);

                dateToProcess = dateToProcess.plusHours(1);
            }
        }

        succeededCounter.inc();
    }

    private Runnable cleanupConversation(DateTime deleteEverythingBefore, ConversationModification conversationModification) {
        return () -> {
            String userId = conversationModification.getUserId();
            String convId = conversationModification.getConversationId();
            DateTime modifiedAt = conversationModification.getModifiedAt();
            ConversationModification lastConversationModification = repository.getLastConversationModification(userId, convId);
            DateTime lastModifiedDate = lastConversationModification.getModifiedAt();
            if (lastModifiedDate != null && (lastModifiedDate.isBefore(deleteEverythingBefore) || lastModifiedDate.equals(deleteEverythingBefore))) {
                try {
                    repository.deleteConversation(userId, lastConversationModification.getAdvertisementId(), convId);
                    deleteConversationCounter.inc();
                } catch (RuntimeException ex) {
                    LOGGER.error("Cleanup: Could not delete Conversation: " + convId, ex);
                    deleteConversationExceptionCounter.inc();
                }
            } else {
                try {
                    repository.deleteModificationIndexByDate(modifiedAt, conversationModification.getMessageId(), userId, convId);
                    deleteModificationIndexCounter.inc();
                } catch (RuntimeException ex) {
                    LOGGER.error("Cleanup: Could not delete modification index for date " + modifiedAt + " and conversation: " + convId, ex);
                    deleteModificationIndexExceptionCounter.inc();
                }
            }
        };
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}
