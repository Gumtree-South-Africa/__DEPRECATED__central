package com.ecg.messagecenter.persistence.block;

import com.ecg.messagecenter.cronjobs.ConversationBlockCleanupCronJob;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.ecg.replyts.core.runtime.persistence.strategy.CassandraPersistenceConfiguration;
import com.ecg.replyts.core.runtime.persistence.strategy.RiakPersistenceConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
@Import({CassandraPersistenceConfiguration.CassandraClientConfiguration.class, RiakPersistenceConfiguration.RiakClientConfiguration.class})
public class HybridConversationBlockConfiguration {

    @Bean
    public ConversationBlockCleanupCronJob cleanupCronJob() {
        return new ConversationBlockCleanupCronJob();
    }

    @Bean
    public CassandraConversationBlockRepository cassandraConversationBlockRepository() {
        return new CassandraConversationBlockRepository();
    }

    @Bean
    public RiakConversationBlockRepository riakConversationBlockRepository() {
        return new RiakConversationBlockRepository();
    }

    @Bean
    public RiakConversationBlockConflictResolver conversationBlockConflictResolver() {
        return new RiakConversationBlockConflictResolver();
    }

    @Bean
    public RiakConversationBlockConverter conversationBlockConverter() {
        return new RiakConversationBlockConverter();
    }

    @Primary
    @Bean
    public ConversationBlockRepository hybridConversationBlockRepository(HybridMigrationClusterState hybridMigrationClusterState) {
        return new HybridConversationBlockRepository(riakConversationBlockRepository(), cassandraConversationBlockRepository(), hybridMigrationClusterState);
    }
}
