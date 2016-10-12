package com.ecg.replyts.core.runtime.migrator;

import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') }")
public class MigratorConfiguration {

    private final Logger LOG = LoggerFactory.getLogger(MigratorConfiguration.class);

    @Autowired
    private HybridConversationRepository conversationRepository;

    @Value("${migration.bulkoperations.threadcount:4}")
    private int threadCount;
    @Value("${migration.chunksize.minutes:1000}")
    private int chunkSizeMinutes;
    @Value("${migration.conversations.maxChunkSize:1000}")
    private int maxConversationChunkSize;
    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Bean
    public MigrationChunkHandler migrationChunkHandler() {
        return new MigrationChunkHandler(conversationRepository, maxConversationChunkSize);
    }

    @Bean
    public ChunkedConversationMigrationAction chunkedConversationMigrationAction(MigrationChunkHandler migrationChunkHandler) {
        LOG.info("Activating r2c migration functionality");
        return new ChunkedConversationMigrationAction(hazelcastInstance, conversationRepository, migrationChunkHandler, threadCount, chunkSizeMinutes, maxConversationAgeDays);
    }
}
