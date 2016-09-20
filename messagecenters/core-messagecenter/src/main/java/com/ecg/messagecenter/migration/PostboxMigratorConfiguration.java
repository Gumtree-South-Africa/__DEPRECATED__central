package com.ecg.messagecenter.migration;

import com.ecg.messagecenter.persistence.simple.HybridSimplePostBoxRepository;
import com.ecg.replyts.core.runtime.indexer.SingleRunGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "r2cmigration", havingValue = "enabled")
public class PostboxMigratorConfiguration {

    private final Logger LOG = LoggerFactory.getLogger(PostboxMigratorConfiguration.class);

    @Autowired
    private HybridSimplePostBoxRepository hybridRepository;

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
    public PostboxMigrationChunkHandler hybridMigrationChunkHandler() {
        return new PostboxMigrationChunkHandler(hybridRepository, maxConversationChunkSize);
    }

    @Bean
    public ChunkedPostboxMigrationAction chunkedMigrationAction(PostboxMigrationChunkHandler postboxMigrationChunkHandler) {
        LOG.info("Activating r2c migration functionality");
        return new ChunkedPostboxMigrationAction(singleRunGuard, hybridRepository, postboxMigrationChunkHandler, threadCount, chunkSizeMinutes, maxConversationAgeDays);
    }

}

