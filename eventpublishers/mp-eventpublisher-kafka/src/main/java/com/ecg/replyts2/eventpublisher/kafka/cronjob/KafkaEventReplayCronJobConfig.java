package com.ecg.replyts2.eventpublisher.kafka.cronjob;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts2.eventpublisher.kafka.KafkaEventPublisherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class KafkaEventReplayCronJobConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventReplayCronJobConfig.class);

    @Autowired
    private CassandraConversationRepository conversationRepository;
    @Autowired
    private MailCloakingService mailCloakingService;
    @Autowired
    private KafkaEventPublisherConfig kafkaEventPublisherConfig;

    @Bean
    @Conditional(value = {KafkaEventPublisherConfig.KafkaEnabledConditional.class, CronJobEnabledConditional.class})
    public KafkaEventReplayCronJob kafkaEventReplayCronJob(
            @Value("${replyts2.cronjob.kafkaEventReplay.cronExpression}") String cronExpression,
            @Value("${replyts2.cronjob.kafkaEventReplay.startDate}") String startDateString,
            @Value("${replyts2.cronjob.kafkaEventReplay.endDate}") String endDateString,
            @Value("${replyts2.cronjob.kafkaEventReplay.streaming.event.batch.size:3000}") int eventBatchSize,
            @Value("${replyts2.cronjob.kafkaEventReplay.streaming.threadcount:4}") int streamingThreadCount,
            @Value("${replyts2.cronjob.kafkaEventReplay.streaming.queue.size:100000}") int workQueueSize) {

        LOGGER.info("KafkaEventReplayCronJob has been enabled with config:" +
                        " startDate: {},  endDate: {}, eventBatchSize: {}, streamingThreadCount: {}, workQueueSize.",
                startDateString,
                endDateString,
                eventBatchSize,
                streamingThreadCount,
                workQueueSize);

        return new KafkaEventReplayCronJob(
                cronExpression,
                conversationRepository,
                startDateString,
                endDateString,
                eventBatchSize,
                streamingThreadCount,
                workQueueSize,
                kafkaEventPublisherConfig.newKafkaEventReplayPublisher(),
                mailCloakingService);
    }

    private static class CronJobEnabledConditional implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Boolean.parseBoolean(context.getEnvironment().getProperty("replyts2.cronjob.kafkaEventReplay.enabled", "false"));
        }
    }
}
