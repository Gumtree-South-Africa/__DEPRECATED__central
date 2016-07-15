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
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;

@Component
@Conditional(CassandraCleanupConversationCronJob.Condition.class)
public class CassandraCleanupConversationCronJob implements CronJobExecutor {
    // Used to determine whether to enable this cron-job (strategy = cassandra && cron-job = enabled)
    public static class Condition extends AllNestedConditions {
        public Condition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = "persistence.strategy", havingValue = "cassandra")
        static class OnCassandraStrategy { }

        @ConditionalOnProperty(name = "replyts2.cleanup.conversation.enabled", havingValue = "true")
        static class OnEnabled { }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CassandraCleanupConversationCronJob.class);

    protected static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;
    @Autowired
    private CronJobClockRepository cronJobClockRepository;
    @Autowired
    private ConversationEventListeners conversationEventListeners;

    private String cronJobExpression;
    private int maxConversationAgeDays;
    private int batchSize;

    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public CassandraCleanupConversationCronJob(
            @Value("${replyts.cleanup.conversation.streaming.queue.size:100000}")
            int workQueueSize,
            @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
            int threadCount,
            @Value("${replyts.cleanup.conversation.maxagedays:120}")
            int maxConversationAgeDays,
            @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
            int batchSize,
            @Value("${replyts.cleanup.conversation.schedule.expression:0 0 0 * * ? *}")
            String cronJobExpression) {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.maxConversationAgeDays = maxConversationAgeDays;
        this.batchSize = batchSize;
        this.cronJobExpression = cronJobExpression;
    }

    @Override
    public void execute() throws Exception {
        final DateTime cleanupDate = createCleanupDateCalculator().getCleanupDate(maxConversationAgeDays, CLEANUP_CONVERSATION_JOB_NAME);
        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date '{}'", cleanupDate);

        Stream<ConversationModificationDate> conversationModificationsToDelete = conversationRepository.streamConversationModificationsByDay(cleanupDate.getYear(),
                cleanupDate.getMonthOfYear(), cleanupDate.getDayOfMonth());

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(conversationModificationsToDelete.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {
                LOG.info("Cleanup: Deleting data related to {} conversation modification dates", idxs.size());
                idxs.forEach(conversationModificationDate -> {
                    String conversationId = conversationModificationDate.getConversationId();
                    DateTime lastModifiedDate = conversationRepository.getLastModifiedDate(conversationId);
                    // it is necessary to compare dates without a time part
                    if ((lastModifiedDate != null) && (lastModifiedDate.toLocalDate().isBefore(cleanupDate.toLocalDate())
                                                        || lastModifiedDate.toLocalDate().equals(cleanupDate.toLocalDate()))) {
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

        cleanUpTasks.stream().filter(task -> !task.isDone()).forEach(task -> {
            try {
                task.get();
            } catch (CancellationException | ExecutionException ignore) {
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

    protected CleanupDateCalculator createCleanupDateCalculator() {
        return new CleanupDateCalculator(cronJobClockRepository);
    }
}
