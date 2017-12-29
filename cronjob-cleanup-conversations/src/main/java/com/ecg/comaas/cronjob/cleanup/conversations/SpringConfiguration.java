package com.ecg.comaas.cronjob.cleanup.conversations;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.CleanupConfiguration;
import com.ecg.replyts.app.cronjobs.cleanup.CassandraCleanupConversationCronJob;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.app.preprocessorchain.preprocessors.MailBasedConversationResumer;
import com.ecg.replyts.core.runtime.CloudDiscoveryConfiguration;
import com.ecg.replyts.core.runtime.LoggingService;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.strategy.CassandraPersistenceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import({CloudDiscoveryConfiguration.class, CassandraPersistenceConfiguration.class})
@PropertySource(name = "confDirProperties", value = "classpath:cronjob.properties")
public class SpringConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public LoggingService loggingService() {
        return new LoggingService();
    }

    @Bean
    public CassandraCleanupConversationCronJob cronJob() {
        return new CassandraCleanupConversationCronJob();
    }

    @Bean
    public CleanupConfiguration cleanupConfiguration(@Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays,
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
        return new CleanupConfiguration(maxConversationAgeDays, maxMailAgeDays, maxResults, everyNMinutes,
                offsetConversations, numCleanUpThreads, workQueueSize, threadCount, conversationCleanupRateLimit,
                readFromNewIndexTable, cleanupTaskTimeoutSec, batchSize, cronJobExpression);
    }

    @Bean
    public ConversationResumer conversationResumer() {
        return new MailBasedConversationResumer();
    }

    @Bean
    public JacksonAwareObjectMapperConfigurer objectMapperConfigurer() {
        return new JacksonAwareObjectMapperConfigurer();
    }

    @Bean
    public ConversationEventListeners conversationEventListeners() {
        return new ConversationEventListeners();
    }

    @Bean
    public CleanupDateCalculator cleanupDateCalculator(@Autowired CronJobClockRepository cronJobClockRepository,
                                                       @Value("${replyts.tenant.timezone:Europe/Amsterdam}") String timeZone,
                                                       @Value("${replyts.cleanup.quietTime.start:16:00}") String quietTimeStart,
                                                       @Value("${replyts.cleanup.quietTime.end:23:00}") String quietTimeEnd) {
        return new CleanupDateCalculator(cronJobClockRepository, timeZone, quietTimeStart, quietTimeEnd);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(@Autowired @Qualifier("cassandraSessionForCore") Session session) {
        return new CassandraCronJobClockRepository(session, ConsistencyLevel.LOCAL_QUORUM, ConsistencyLevel.LOCAL_QUORUM);
    }
}
