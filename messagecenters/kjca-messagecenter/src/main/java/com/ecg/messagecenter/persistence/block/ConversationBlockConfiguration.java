package com.ecg.messagecenter.persistence.block;

import com.ecg.messagecenter.cronjobs.ConversationBlockCleanupCronJob;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversationBlockConfiguration {

    @Value("${persistence.strategy}")
    private String persistenceStrategy;

    @Bean
    public ConversationBlockCleanupCronJob cleanupCronJob() {
        return new ConversationBlockCleanupCronJob();
    }

    @Bean
    public ConversationBlockRepository conversationBlockRepository(HybridMigrationClusterState hybridMigrationClusterState) {
        if (persistenceStrategy.equalsIgnoreCase("riak")) {
            return new RiakConversationBlockRepository();
        } else if (persistenceStrategy.equalsIgnoreCase("cassandra")) {
            return new CassandraConversationBlockRepository();
        } else if (persistenceStrategy.equalsIgnoreCase("hybrid")) {
            return new HybridConversationBlockRepository(new RiakConversationBlockRepository(), new CassandraConversationBlockRepository(), hybridMigrationClusterState);
        } else {
            throw new IllegalArgumentException("Incorrect persistence strategy '" + persistenceStrategy + "'");
        }
    }
}
