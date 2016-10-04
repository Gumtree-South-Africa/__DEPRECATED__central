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
    @Primary
    public ConversationBlockRepository conversationBlockRepository(RiakConversationBlockRepository riakRepository, CassandraConversationBlockRepository cassandraRepository, HybridMigrationClusterState migrationState) {
        return new HybridConversationBlockRepository(riakRepository, cassandraRepository, migrationState);
    }
}
