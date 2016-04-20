package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import com.ecg.replyts.core.runtime.persistence.conditional.CassandraEnabledConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
class CleanupConversationConfiguration {

    @Value("${replyts.cleanup.conversation.maxagedays:120}")
    private int maxConversationAgeDays;
    /**
     * Should be according to quartz cron expression
     */
    @Value("${replyts.cleanup.conversation.schedule.expression:0 0 0 * * ? *}")
    private String cronJobExpression;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int streamingThreadCount;
    @Value("${replyts.cleanup.conversation.streaming.queue.size:100000}")
    private int workQueueSize;
    @Value("${replyts.cleanup.conversation.streaming.batch.size:3000}")
    private int streamingBatchSize;

    @Bean
    @Conditional(value = {CassandraEnabledConditional.class, CleanupConversationCronJobEnabledConditional.class})
    public CassandraCleanupConversationCronJob cassandraCleanupConversationCronJob() {
        return new CassandraCleanupConversationCronJob(workQueueSize, streamingThreadCount, maxConversationAgeDays, streamingBatchSize,
                cronJobExpression);
    }
}
