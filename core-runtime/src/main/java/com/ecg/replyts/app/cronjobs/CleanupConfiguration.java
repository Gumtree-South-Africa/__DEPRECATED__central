package com.ecg.replyts.app.cronjobs;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CleanupConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupConfiguration.class);

    private final int maxConversationAgeDays;
    private final int maxMailAgeDays;
    private final int maxResults;
    private final int everyNMinutes;
    private final int offsetConversations;
    private final int numCleanUpThreads;
    private final int workQueueSize;
    private final int threadCount;
    private final int conversationCleanupRateLimit;
    private final int cleanupTaskTimeoutSec;
    private final int batchSize;
    private final String cronJobExpression;

    @Autowired
    public CleanupConfiguration(
            @Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays,
            @Value("${replyts.maxMailAgeDays:#{null}}") Integer maxMailAgeDays,
            @Value("${replyts.CleanupCronJob.maxresults:100000}") int maxResults,
            @Value("${replyts.CleanupCronJob.everyNMinutes:30}") int everyNMinutes,
            @Value("${replyts.CleanupCronJob.offsetConversations:15}") int offsetConversations,
            @Value("${replyts.CleanupCronJob.numCleanUpThreads:2}") int numCleanUpThreads,
            @Value("${replyts.cleanup.conversation.streaming.queue.size:100}") int workQueueSize,
            @Value("${replyts.cleanup.conversation.streaming.threadcount:4}") int threadCount,
            @Value("${replyts.cleanup.conversation.rate.limit:1000}") int conversationCleanupRateLimit,
            @Value("${replyts.cleanup.task.timeout.sec:60}") int cleanupTaskTimeoutSec,
            @Value("${replyts.cleanup.conversation.streaming.batch.size:2000}") int batchSize,
            @Value("${replyts.cleanup.conversation.schedule.expression:0 0/30 * * * ? *}") String cronJobExpression) {
        this.workQueueSize = workQueueSize;
        this.threadCount = threadCount;
        this.conversationCleanupRateLimit = conversationCleanupRateLimit;
        this.cleanupTaskTimeoutSec = cleanupTaskTimeoutSec;
        this.batchSize = batchSize;
        this.cronJobExpression = cronJobExpression;
        Preconditions.checkArgument(maxConversationAgeDays > 0);
        Preconditions.checkArgument(maxResults > 0);
        Preconditions.checkArgument(everyNMinutes > 0);
        Preconditions.checkArgument(offsetConversations > 0);
        Preconditions.checkArgument(numCleanUpThreads > 0);
        Preconditions.checkArgument(maxMailAgeDays == null || maxMailAgeDays > 0);
        this.maxConversationAgeDays = maxConversationAgeDays;
        // For backward compatibility, we take value of `maxConversationAgeDays` when maxMailAgeDays was not set
        this.maxMailAgeDays = maxMailAgeDays == null ? maxConversationAgeDays : maxMailAgeDays;
        this.maxResults = maxResults;
        this.everyNMinutes = everyNMinutes;
        this.offsetConversations = offsetConversations;
        this.numCleanUpThreads = numCleanUpThreads;

        LOG.info("Cleanup config maxConversationAgeDays: {},  maxMailAgeDays: {}, maxResults (page size): {}, everyNMinutes: {}, delayForConversations: {}, numCleanupThreads: {}.",
                maxConversationAgeDays,
                maxMailAgeDays,
                maxResults,
                everyNMinutes,
                offsetConversations,
                numCleanUpThreads);
    }

    public int getMaxConversationAgeDays() {
        return maxConversationAgeDays;
    }

    /**
     * @return Actually, this is the page size of index values loaded in the memory.
     */
    public int getMaxResults() {
        return maxResults;
    }

    public int getEveryNMinutes() {
        return everyNMinutes;
    }

    public int getOffsetConversations() {
        return offsetConversations;
    }

    public int getNumCleanUpThreads() {
        return numCleanUpThreads;
    }

    public int getWorkQueueSize() {
        return workQueueSize;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getConversationCleanupRateLimit() {
        return conversationCleanupRateLimit;
    }

    public int getCleanupTaskTimeoutSec() {
        return cleanupTaskTimeoutSec;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public String getCronJobExpression() {
        return cronJobExpression;
    }
}
