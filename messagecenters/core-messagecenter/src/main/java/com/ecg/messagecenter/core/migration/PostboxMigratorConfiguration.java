package com.ecg.messagecenter.core.migration;

import com.ecg.messagecenter.core.persistence.simple.HybridSimplePostBoxRepository;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') }")
public class PostboxMigratorConfiguration {
    private final Logger LOG = LoggerFactory.getLogger(PostboxMigratorConfiguration.class);

    @Autowired
    private HybridSimplePostBoxRepository hybridRepository;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Value("${migration.threadcount:4}")
    private int threadCount;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Value("${migration.batch.size:1000}")
    private int idBatchSize;

    @Value("${migration.queue.size:100}")
    private int workQueueSize;

    @Value("${migration.completion.timeout.sec:60}")
    private int completionTimeoutSec;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(workQueueSize), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    public ChunkedPostboxMigrationAction chunkedPostboxMigrationAction(ThreadPoolExecutor threadPoolExecutor) {
        LOG.info("Activating r2c migration functionality");

        return new ChunkedPostboxMigrationAction(hazelcastInstance, hybridRepository, threadPoolExecutor, idBatchSize, maxConversationAgeDays, completionTimeoutSec);
    }
}
