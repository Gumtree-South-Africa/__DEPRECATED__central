package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.never;
import static com.ecg.replyts.core.runtime.persistence.FetchIndexHelper.logInterval;
import static org.joda.time.DateTime.now;

public class CleanupConversationCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupConversationCronJob.class);

    private final boolean cronJobEnabled;
    private final MutableConversationRepository conversationRepository;
    private final CleanupConfiguration config;
    private final ConversationEventListeners conversationEventListeners;

    @Autowired
    CleanupConversationCronJob(
            @Value("${replyts2.cronjob.cleanupConversation.enabled:true}") boolean cronJobEnabled,
            MutableConversationRepository conversationRepository,
            CleanupConfiguration config,
            ConversationEventListeners conversationEventListeners) {
        this.cronJobEnabled = cronJobEnabled;
        this.conversationRepository = conversationRepository;
        this.config = config;
        this.conversationEventListeners = conversationEventListeners;
    }

    @Override
    public void execute() throws Exception {
        final long startedAt = System.currentTimeMillis();
        final DateTime deleteEverythingBefore = now().minusDays(config.getMaxConversationAgeDays());

        final long logInterval = logInterval(config.getMaxResults());
        final AtomicLong deleteCounter = new AtomicLong();

        LOG.info("Cleanup: Deleting conversations older than {} days, everything before '{}'", config.getMaxConversationAgeDays(), deleteEverythingBefore);

        final Set<String> conversationsToDelete;
        try {
            conversationsToDelete = conversationRepository.getConversationsModifiedBefore(deleteEverythingBefore, config.getMaxResults());
        } catch (RuntimeException e) {
            LOG.error("Cleanup: Failed to fetch conversations to delete", e);
            return;
        }

        LOG.info("Cleanup: Found {} conversations to delete", conversationsToDelete.size());

        try {
            new BlockingBatchExecutor<String>("cleanup-conversation", 2, 2, TimeUnit.HOURS).executeAll(
                    conversationsToDelete,
                    conversationId -> () -> {
                        try {
                            MutableConversation conversation = conversationRepository.getById(conversationId);
                            conversation.applyCommand(new ConversationDeletedCommand(conversation.getId(), now()));
                            ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
                            logProgress(startedAt, logInterval, deleteCounter);

                        } catch (RuntimeException ex) {
                            LOG.error("Cleanup: Could not delete Conversation: " + conversationId, ex);
                        }
                    }
            );

            LOG.info("Cleanup: Deleted {} conversations", deleteCounter);

        } catch (RuntimeException e) {
            LOG.error("Cleanup: Failed, deleted {} of {} conversations", deleteCounter, conversationsToDelete.size(), e);
        }
    }

    private void logProgress(long startedAt, long logInterval, AtomicLong deleteCounter) {
        long count = deleteCounter.incrementAndGet();
        if (count % logInterval == 0) {
            long duration = new Duration(startedAt, System.currentTimeMillis()).getStandardSeconds();
            double rate = ((double) count) / ((double) (duration));
            LOG.info("Cleanup: deleted {} conversations in {}s. {}/s", count, duration, rate);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        if (!cronJobEnabled) {
            return never();
        }
        return everyNMinutes(config.getEveryNMinutes(), config.getOffsetConversations());
    }
}
