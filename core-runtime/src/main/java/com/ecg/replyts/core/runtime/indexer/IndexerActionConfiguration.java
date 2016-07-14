package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexerActionConfiguration {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private IndexerChunkHandler indexerChunkHandler;

    // ChunkedIndexer properties 
    @Value("${batch.bulkoperations.threadcount:4}")
    private int chunkedThreadCount;
    @Value("${batch.indexer.chunksize.minutes:10}")
    private int chunkSizeMinutes;

    // StreamingIndexer  properties
    @Value("${replyts.indexer.streaming.threadcount:4}")
    private int streamingThreadCount;
    @Value("${replyts.indexer.streaming.queue.size:100000}")
    private int workQueueSize;
    @Value("${replyts.indexer.streaming.maxWaitingTasks:30}")
    private int maxWaitingTasks;
    @Value("${replyts.indexer.streaming.conversationid.batch.size:3000}")
    private int conversationIdBatchSize;
    
    @Bean
    @Conditional(StreamingIndexerActionConditional.class)
    public IndexerAction streamingIndexer() {
        return new StreamingIndexerAction(conversationRepository, indexerChunkHandler, streamingThreadCount, workQueueSize, conversationIdBatchSize, maxWaitingTasks);
    }

    @Bean
    @Conditional(ChunkedIndexerActionConditional.class)
    public IndexerAction chunkedIndexer() {
        return new ChunkedIndexerAction(conversationRepository, indexerChunkHandler, chunkedThreadCount, chunkSizeMinutes);
    }
}