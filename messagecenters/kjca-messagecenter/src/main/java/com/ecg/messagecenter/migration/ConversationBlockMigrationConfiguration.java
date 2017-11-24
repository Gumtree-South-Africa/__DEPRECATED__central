package com.ecg.messagecenter.migration;

import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(value = "persistence.strategy", havingValue = "hybrid")
public class ConversationBlockMigrationConfiguration {

    @Autowired
    private ConversationBlockRepository conversationBlockRepository;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Value("${migration.threadcount:4}")
    private int threadCount;

    @Value("${migration.queue.size:100}")
    private int workQueueSize;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(workQueueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    public ConversationBlockMigration conversationBlockMigration() {
        return new ConversationBlockMigration(hazelcastInstance, conversationBlockRepository, threadPoolExecutor());
    }
}
