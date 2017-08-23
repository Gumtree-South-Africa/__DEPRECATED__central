package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;

import static com.ecg.replyts.core.runtime.TimingReports.newGauge;

@Component
@ConditionalOnExpression("#{" +
            "'${replyts2.cleanup.conversation.enabled}' == '${region}' && " +
            "('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid'))" +
        "}")
public class CassandraCleanupConversationCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraCleanupConversationCronJob.class);

    private static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Value("${replyts.maxConversationAgeDays:120}")
    private int maxAgeDays;

    @Value("${replyts.cleanup.conversation.streaming.queue.size:100000}")
    private int workQueueSize;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int threadCount;

    @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
    private int batchSize;

    @Value("${replyts.cleanup.conversation.schedule.expression:0 0 0 * * ? *}")
    private String cronJobExpression;

    private ThreadPoolExecutor threadPoolExecutor;

    @Value("${replyts.cleanup.conversation.rate.limit:30000}")
    private int conversationCleanupRateLimit;

    private RateLimiter rateLimiter;

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.rateLimiter = RateLimiter.create(conversationCleanupRateLimit);
    }

    @Override
    public void execute() throws Exception {
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(maxAgeDays, CLEANUP_CONVERSATION_JOB_NAME);

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date '{}'", cleanupDate);

        Stream<ConversationEventIdx> conversationEventIdxs = conversationRepository.streamConversationEventIdxsByHour(cleanupDate);

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(conversationEventIdxs.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {
                LOG.info("Cleanup: Deleting data related to {} conversation events", idxs.size());

                idxs.forEach(conversationEventIdx -> {

                    double sleepTimeSeconds = rateLimiter.acquire();
                    newGauge("cleanup.conversations.rateLimiter.sleepTimeSecondsGauge", () -> sleepTimeSeconds);

                    String conversationId = conversationEventIdx.getConversationId();
                    Long lastModifiedDate = conversationRepository.getLastModifiedDate(conversationId);

                    // Round the lastModifiedDate to the day, then compare to the (already rounded) cleanup date

                    DateTime roundedLastModifiedDate = lastModifiedDate != null ? new DateTime((lastModifiedDate)).hourOfDay().roundFloorCopy().toDateTime() : null;

                    if (lastModifiedDate != null && (roundedLastModifiedDate.isBefore(cleanupDate) || roundedLastModifiedDate.equals(cleanupDate))) {
                        try {
                            deleteConversationWithIdxs(conversationEventIdx);

                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete Conversation: " + conversationEventIdx.toString(), ex);
                        }
                    } else {
                        try {
                            conversationRepository.deleteConversationEventIdx(conversationEventIdx);

                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete conversation event index " + conversationEventIdx.toString(), ex);
                        }
                    }
                });
            }));
        });

        for (Future<?> task : cleanUpTasks) {
            try {
                task.get();
            } catch (ExecutionException | RuntimeException e) {
                LOG.error("Conversation cleanup task execution failure", e);
            } catch (InterruptedException e) {
                LOG.warn("The cleanup task has been interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }

        cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);

        LOG.info("Cleanup: Finished deleting conversations.");
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }

    private void deleteConversationWithIdxs(ConversationEventIdx conversationEventIdx) {
        MutableConversation conversation = conversationRepository.getById(conversationEventIdx.getConversationId());
        if (conversation != null) {
            conversation.applyCommand(new ConversationDeletedCommand(conversation.getId(), now()));
            ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        } else {
            conversationRepository.deleteConversationModificationIdxs(conversationEventIdx.getConversationId());
            conversationRepository.deleteConversationEventIdx(conversationEventIdx);
        }
    }
}