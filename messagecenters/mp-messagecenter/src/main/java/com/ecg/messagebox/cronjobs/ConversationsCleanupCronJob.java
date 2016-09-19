package com.ecg.messagebox.cronjobs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagebox.util.InstrumentedExecutorService;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.*;
import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("#{'${messagebox.cronjob.conversationsCleanup.enabled}' == 'true'}")
public class ConversationsCleanupCronJob implements CronJobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationsCleanupCronJob.class);

    private static final DateTime STARTING_DATE = new DateTime(1445774400000L); // 2015-10-25 13:00:00
    protected static final String CONVERSATIONS_CLEANUP_CRONJOB_NAME = "messagebox_conversations_cleanup_cronjob";

    private final Timer processHourTimer = newTimer("conversationsCleanupCronjob.processHourTimer");
    private final Counter deleteConversationCounter = newCounter("conversationsCleanupCronjob.deleteConversationCounter");
    private final Counter deleteModificationIndexCounter = newCounter("conversationsCleanupCronjob.deleteModificationIndexByDateCounter");
    private final Counter failedCounter = newCounter("conversationsCleanupCronjob.failedCounter");

    private final int maxConversationAgeDays;
    private final int batchSize;
    private final CassandraPostBoxRepository repository;
    private final String cronJobExpression;
    private final ExecutorService executorService;
    private final CronJobClockRepository cronJobClockRepository;
    private final RateLimiter rateLimiter;

    @Autowired
    ConversationsCleanupCronJob(@Qualifier("newCassandraPostBoxRepo") CassandraPostBoxRepository repository,
                                CronJobClockRepository cronJobClockRepository,
                                @Value("${messagebox.cronjob.conversationsCleanup.workQueueSize:400}") int workQueueSize,
                                @Value("${messagebox.cronjob.conversationsCleanup.threadCount:4}") int threadCount,
                                @Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays,
                                @Value("${messagebox.cronjob.conversationsCleanup.batchSize:300}") int batchSize,
                                @Value("${messagebox.cronjob.conversationsCleanup.expression:0 0 0 * * ? *}") String cronJobExpression,
                                @Value("${messagebox.cronjob.conversationsCleanup.deletionRateLimit:1000}") int deletionRateLimit
    ) {
        this.cronJobClockRepository = cronJobClockRepository;

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        newGauge("conversationsCleanupCronjob.executionService.workQueueSizeGauge", workQueue::size);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("conversationsCleanupCronjob", "rejectionHandler");
        this.executorService = new InstrumentedExecutorService(
                new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler),
                "conversationsCleanupCronjob", "executionService");

        this.rateLimiter = RateLimiter.create(deletionRateLimit);

        this.repository = repository;
        this.cronJobExpression = cronJobExpression;
        this.maxConversationAgeDays = maxConversationAgeDays;
        this.batchSize = batchSize;
    }

    @Override
    public void execute() throws Exception {
        final DateTime deleteEverythingBefore = now().minusDays(maxConversationAgeDays);
        DateTime lastProcessedDate = cronJobClockRepository.getLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME);
        DateTime dateToProcess = lastProcessedDate == null ? STARTING_DATE : lastProcessedDate.plusHours(1);

        while (dateToProcess.isBefore(deleteEverythingBefore)) {
            try (Timer.Context ignored = processHourTimer.time()) {
                Stream<ConversationModification> conversationModifications = repository.getConversationModificationsByHour(dateToProcess);
                Iterator<List<ConversationModification>> partitions = Iterators.partition(conversationModifications.iterator(), batchSize);

                LOGGER.info("{}: Deleting conversations for {}", CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess);

                while (partitions.hasNext()) {
                    List<Future> cleanupTasks = partitions.next().stream().
                            map(conv -> cleanupConversation(deleteEverythingBefore, conv)).
                            map(executorService::submit).
                            collect(Collectors.toList());

                    for (Future task : cleanupTasks) {
                        try {
                            task.get(1, TimeUnit.SECONDS);
                        } catch (InterruptedException | ExecutionException e) {
                            failedCounter.inc();
                            LOGGER.error("{} failed for {}", CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess, e);
                            return;
                        }
                    }
                }

                cronJobClockRepository.set(CONVERSATIONS_CLEANUP_CRONJOB_NAME, now(), dateToProcess);

                dateToProcess = dateToProcess.plusHours(1);
            }
        }
    }

    private Runnable cleanupConversation(DateTime deleteEverythingBefore, ConversationModification conversationModification) {
        return () -> {
            rateLimiter.acquire();
            String userId = conversationModification.getUserId();
            String convId = conversationModification.getConversationId();
            DateTime modifiedAt = conversationModification.getModifiedAt();
            ConversationModification lastConversationModification = repository.getLastConversationModification(userId, convId);
            if (lastConversationModification != null && lastConversationModification.getModifiedAt().isBefore(deleteEverythingBefore)) {
                repository.deleteConversations(userId, Collections.singletonMap(lastConversationModification.getAdvertisementId(), convId));
                deleteConversationCounter.inc();
            } else {
                repository.deleteModificationIndexByDate(modifiedAt, conversationModification.getMessageId(), userId, convId);
                deleteModificationIndexCounter.inc();
            }
        };
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}