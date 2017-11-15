package com.ecg.messagecenter.persistence.block;

import com.ecg.messagecenter.cronjobs.ConversationBlockCleanupCronJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "persistence.strategy", havingValue = "cassandra")
public class CassandraConversationBlockConfiguration {

    @Bean
    public ConversationBlockCleanupCronJob cleanupCronJob() {
        return new ConversationBlockCleanupCronJob();
    }

    @Bean
    public ConversationBlockRepository conversationBlockRepository() {
        return new CassandraConversationBlockRepository();
    }
}
