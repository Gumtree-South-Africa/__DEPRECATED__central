package com.ecg.replyts.core.runtime.migrator;

import com.ecg.replyts.core.runtime.indexer.SingleRunGuard;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "r2cmigration", havingValue = "enabled")
public class MigratorConfiguration {

    private final Logger LOG = LoggerFactory.getLogger(MigratorConfiguration.class);

    @Autowired
    private HybridConversationRepository conversationRepository;

    @Autowired
    private SingleRunGuard singleRunGuard;

    @Value("${migration.bulkoperations.threadcount:4}")
    private int threadCount;
    @Value("${migration.chunksize.minutes:1000}")
    private int chunkSizeMinutes;
    @Value("${migration.conversations.maxChunkSize:1000}")
    private int maxConversationChunkSize;
    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;


    @Bean
    public MigrationChunkHandler migrationChunkHandler() {
        return new MigrationChunkHandler(conversationRepository, maxConversationChunkSize);
    }

    @Bean
    public ChunkedConversationMigrationAction chunkedMigrationAction(MigrationChunkHandler migrationChunkHandler) {
        LOG.info("Activating r2c migration functionality");
        return new ChunkedConversationMigrationAction(singleRunGuard, conversationRepository, migrationChunkHandler, threadCount, chunkSizeMinutes, maxConversationAgeDays);
    }
}
