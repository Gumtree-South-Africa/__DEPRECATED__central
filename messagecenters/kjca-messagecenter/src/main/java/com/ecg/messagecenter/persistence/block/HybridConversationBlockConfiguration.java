package com.ecg.messagecenter.persistence.block;

import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
public class HybridConversationBlockConfiguration {
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