package com.ecg.replyts.core.runtime.migrator;

import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
public class MigratorConfiguration {

    private final Logger LOG = LoggerFactory.getLogger(MigratorConfiguration.class);

    @Autowired
    private HybridConversationRepository conversationRepository;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Value("${migration.threadcount:4}")
    private int threadCount;

    @Value("${migration.batch.size:1000}")
    private int idBatchSize;

    @Value("${migration.queue.size:100}")
    private int workQueueSize;

    @Value("${migration.completion.timeout.sec:60}")
    private int completionTimeoutSec;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(workQueueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    public ChunkedConversationMigrationAction chunkedConversationMigrationAction(ThreadPoolExecutor threadPoolExecutor) {
        LOG.info("Activating r2c migration functionality");
        return new ChunkedConversationMigrationAction(hazelcastInstance, conversationRepository,
                 maxConversationAgeDays, threadPoolExecutor, idBatchSize, completionTimeoutSec);
    }

    @Bean
    @ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
    public MailAttachmentMigrator mailAttachmentMigrator() {
        return new MailAttachmentMigrator(idBatchSize, maxConversationAgeDays, completionTimeoutSec);
    }

    @Bean
    @ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
    public MailAttachmentVerifier mailAttachmentVerifier() {
        return new MailAttachmentVerifier(idBatchSize, completionTimeoutSec);
    }
}
