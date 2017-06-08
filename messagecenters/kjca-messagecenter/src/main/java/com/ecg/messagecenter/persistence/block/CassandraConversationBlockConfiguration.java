package com.ecg.messagecenter.persistence.block;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.cronjobs.ConversationBlockCleanupCronJob;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid')}")
public class CassandraConversationBlockConfiguration {
    @Bean
    public ConversationBlockCleanupCronJob cleanupCronJob() {
        return new ConversationBlockCleanupCronJob();
    }

    @Bean
    public CassandraConversationBlockRepository conversationBlockRepository(@Qualifier("cassandraSessionForMb") Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraConversationBlockRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }
}