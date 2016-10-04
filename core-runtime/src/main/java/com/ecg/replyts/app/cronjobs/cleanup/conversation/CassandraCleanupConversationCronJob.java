package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.collect.Iterators;
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

@Component
@ConditionalOnExpression("#{('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid')) && '${replyts2.cleanup.conversation.enabled}' == 'true'}")
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

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
    }

    @Override
    public void execute() throws Exception {
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(maxAgeDays, CLEANUP_CONVERSATION_JOB_NAME);

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date '{}'", cleanupDate);

        Stream<ConversationModificationDate> conversationModificationsToDelete = conversationRepository.streamConversationModificationsByDay(
          cleanupDate.getYear(), cleanupDate.getMonthOfYear(), cleanupDate.getDayOfMonth());

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(conversationModificationsToDelete.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {
                LOG.info("Cleanup: Deleting data related to {} conversation modification dates", idxs.size());

                idxs.forEach(conversationModificationDate -> {
                    String conversationId = conversationModificationDate.getConversationId();
                    DateTime lastModifiedDate = conversationRepository.getLastModifiedDate(conversationId);

                    // Round the lastModifiedDate to the day, then compare to the (already rounded) cleanup date

                    DateTime roundedLastModifiedDate = lastModifiedDate != null ? lastModifiedDate.dayOfMonth().roundFloorCopy().toDateTime() : null;

                    if (lastModifiedDate != null && (roundedLastModifiedDate.isBefore(cleanupDate) || roundedLastModifiedDate.equals(cleanupDate))) {
                        try {
                            deleteConversationWithModificationIdx(conversationId);

                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete Conversation: " + conversationId, ex);
                        }
                    } else {
                        try {
                            conversationRepository.deleteOldConversationModificationDate(conversationModificationDate);

                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete " + conversationModificationDate.toString(), ex);
                        }
                    }
                });
            }));
        });

        cleanUpTasks.stream().forEach(task -> {
            try {
                task.get();
            } catch (ExecutionException | RuntimeException e) {
                LOG.error("Conversation cleanup task execution failure", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);

        LOG.info("Cleanup: Finished deleting conversations.");
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }

    private void deleteConversationWithModificationIdx(String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation != null) {
            conversation.applyCommand(new ConversationDeletedCommand(conversation.getId(), now()));
            ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        }
    }
}