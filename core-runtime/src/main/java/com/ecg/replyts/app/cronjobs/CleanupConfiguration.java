package com.ecg.replyts.app.cronjobs;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class CleanupConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupConfiguration.class);

    private final int maxConversationAgeDays;
    private final int maxMailAgeDays;
    private final int maxResults;
    private final int everyNMinutes;
    private final int offsetConversations;
    private final int numCleanUpThreads;

    public int getMaxConversationAgeDays() {
        return maxConversationAgeDays;
    }

    public int getMaxMailAgeDays() {
        return maxMailAgeDays;
    }

    /**
     * @return Actually, this is the page size of index values loaded in the memory. Unfortunately Riak call this 'maxResults'.
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

    @Autowired
    public CleanupConfiguration(
            @Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays,
            @Value("${replyts.maxMailAgeDays:#{null}}") Integer maxMailAgeDays,
            @Value("${replyts.CleanupCronJob.maxresults:100000}") int maxResults,
            @Value("${replyts.CleanupCronJob.everyNMinutes:30}") int everyNMinutes,
            @Value("${replyts.CleanupCronJob.offsetConversations:15}") int offsetConversations,
            @Value("${replyts.CleanupCronJob.numCleanUpThreads:2}") int numCleanUpThreads
    ) {
        Preconditions.checkArgument(maxConversationAgeDays > 0 && maxResults > 0 && everyNMinutes > 0 && offsetConversations > 0 && numCleanUpThreads > 0);
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

}
