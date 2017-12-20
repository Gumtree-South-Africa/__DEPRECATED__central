package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.CleanupConfiguration;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

public class CassandreConversationCleanupTestContext {

    @Bean
    public CleanupConfiguration cassandraConversationCleanupConfig(@Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays,
                                                                   @Value("${replyts.maxMailAgeDays:#{null}}") Integer maxMailAgeDays,
                                                                   @Value("${replyts.CleanupCronJob.maxresults:100000}") int maxResults,
                                                                   @Value("${replyts.CleanupCronJob.everyNMinutes:30}") int everyNMinutes,
                                                                   @Value("${replyts.CleanupCronJob.offsetConversations:15}") int offsetConversations,
                                                                   @Value("${replyts.CleanupCronJob.numCleanUpThreads:2}") int numCleanUpThreads,
                                                                   @Value("${replyts.cleanup.conversation.streaming.queue.size:100}") int workQueueSize,
                                                                   @Value("${replyts.cleanup.conversation.streaming.threadcount:4}") int threadCount,
                                                                   @Value("${replyts.cleanup.conversation.rate.limit:1000}") int conversationCleanupRateLimit,
                                                                   @Value("${cronjob.cleanup.conversation.readFromNewIndexTable:false}") boolean readFromNewIndexTable,
                                                                   @Value("${replyts.cleanup.task.timeout.sec:60}") int cleanupTaskTimeoutSec,
                                                                   @Value("${replyts.cleanup.conversation.streaming.batch.size:2000}") int batchSize,
                                                                   @Value("${replyts.cleanup.conversation.schedule.expression:0 0/30 * * * ? *}") String cronJobExpression) {

        return new CleanupConfiguration(maxConversationAgeDays, maxMailAgeDays, maxResults, everyNMinutes, offsetConversations,
                numCleanUpThreads, workQueueSize, threadCount, conversationCleanupRateLimit, readFromNewIndexTable, cleanupTaskTimeoutSec,
                batchSize, cronJobExpression);
    }

    @Bean
    public CassandraConversationRepository conversationRepository() {
        return mock(CassandraConversationRepository.class);
    }

    @Bean
    public CassandraHeldMailRepository cassandraHeldMailRepository() {
        return mock(CassandraHeldMailRepository.class);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository() {
        return mock(CronJobClockRepository.class);
    }

    @Bean
    public ConversationEventListeners conversationEventListeners() {
        return mock(ConversationEventListeners.class);
    }

    @Bean
    public CleanupDateCalculator cleanupDateCalculator() {
        return mock(CleanupDateCalculator.class);
    }

    @Bean
    public CassandraCleanupConversationCronJob cleanupCronJob() {
        return new CassandraCleanupConversationCronJob();
    }
}
