package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.google.common.collect.Lists;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IndexerChunkHandler {

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("index-chunk");
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-chunk");

    private final ConversationRepository conversationRepository;
    private final SearchIndexer indexer;
    private final int maxChunkSize;

    private static final Logger LOG = LoggerFactory.getLogger(IndexerChunkHandler.class);
    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    @Autowired
    IndexerChunkHandler(
            ConversationRepository conversationRepository,
            SearchIndexer indexer,
            @Value("${batch.bulkoperations.maxChunkSize:350}") int maxChunkSize) {
        this.conversationRepository = conversationRepository;
        this.indexer = indexer;
        this.maxChunkSize = maxChunkSize;
    }

    public void indexChunk(List<String> conversationIds) {
        if (conversationIds.size() > maxChunkSize) {
            LOG.info("Partitioning conversation list with {} elements into chunks of size {}", conversationIds.size(), maxChunkSize);
        }
        List<List<String>> partitions = Lists.partition(conversationIds, maxChunkSize);

        for (List<String> partition : partitions) {
            indexChunkPartition(partition);
        }
    }

    private void indexChunkPartition(List<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return;
        }
        try (Timer.Context timer = OVERALL_TIMER.time()) {
            List<Conversation> conversations = fetchConversations(conversationIds);
            indexer.updateSearchSync(conversations);
        }
    }

    public List<ListenableActionFuture<BulkResponse>> indexChunkAsync(Set<String> conversationIds) {
        if (conversationIds.size() > maxChunkSize) {
            LOG.info("Partitioning conversation list with {} elements into chunks of size {}", conversationIds.size(), maxChunkSize);
        }

        List<List<String>> partitions = Lists.partition(new ArrayList<>(conversationIds), maxChunkSize);
        List<ListenableActionFuture<BulkResponse>> indexTasks = Lists.newArrayListWithExpectedSize(partitions.size());
        for (List<String> partition : partitions) {
            indexTasks.add( indexChunkPartitionAsync(partition) );
        }
        return indexTasks;
    }

    public List<Conversation> fetchConversations(List<String> conversationIds) {
        List<Conversation> conversations = new ArrayList<>();
        for (String convId : conversationIds) {
            try {
                MutableConversation conversation = conversationRepository.getById(convId);
                // might be null for very old conversation that have been removed by the cleanup job while the indexer
                // was running.
                if (conversation != null) {
                    conversations.add(conversation);
                }
            } catch (Exception e) {
                LOG.error("Indexer could not load conversation '" + convId + "' from repository - skipping it", e);
                FAILED_IDX.info(convId);
            }
        }
        if(conversations.size() > 0) {
            LOG.trace("Fetch {} conversations from {} to {} completed", conversationIds.size(), conversationIds.get(0), conversationIds.get(conversationIds.size() - 1));
        }
        return conversations;
    }


    private ListenableActionFuture<BulkResponse> indexChunkPartitionAsync(List<String> conversationIds) {
        try (Timer.Context timer = FETCH_TIMER.time()) {
            List<Conversation> conversations = fetchConversations(conversationIds);
            if(conversations.size() != conversationIds.size()) {
                LOG.warn("At least some conversation IDs were not found in the database, {} conversations expected, but only {} retrieved",
                        conversationIds.size(), conversations.size());
            }
            return indexer.updateSearchAsync(conversations, false);
        }
    }


}