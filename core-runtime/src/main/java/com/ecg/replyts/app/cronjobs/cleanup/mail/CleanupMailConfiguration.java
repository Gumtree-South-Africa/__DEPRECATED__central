package com.ecg.replyts.app.cronjobs.cleanup.mail;

import com.ecg.replyts.core.runtime.persistence.conditional.CassandraEnabledConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
class CleanupMailConfiguration {

    @Value("${replyts.cleanup.mail.maxagedays:120}")
    private int maxAgeDays;
    /**
     * Should be according to quartz cron expression
     */
    @Value("${replyts.cleanup.mail.schedule.expression:0 0 0 * * ? *}")
    private String cronJobExpression;

    @Value("${replyts.cleanup.mail.streaming.threadcount:4}")
    private int streamingThreadCount;
    @Value("${replyts.cleanup.mail.streaming.queue.size:100000}")
    private int workQueueSize;
    @Value("${replyts.cleanup.mail.streaming.batch.size:3000}")
    private int streamingBatchSize;

    @Bean
    @Conditional(value = {CassandraEnabledConditional.class, CleanupMailCronJobEnabledConditional.class})
    public CassandraCleanupMailCronJob cassandraCleanupMailCronJob() {
        return new CassandraCleanupMailCronJob(workQueueSize, streamingThreadCount, maxAgeDays, streamingBatchSize,
                cronJobExpression);
    }
}
