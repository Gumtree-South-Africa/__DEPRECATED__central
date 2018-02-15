package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndexerChunkHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerChunkHandler.class);
    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("index-chunk");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private SearchIndexer indexer;

    @Value("${batch.bulkoperations.maxChunkSize:200}")
    private int maxChunkSize;

    public void indexChunk(List<String> conversationIds) {
        if (conversationIds.size() > maxChunkSize) {
            LOG.trace("Partitioning conversation list with {} elements into chunks of size {}", conversationIds.size(), maxChunkSize);
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

        try (Timer.Context ignore = OVERALL_TIMER.time()) {
            List<Conversation> conversations = fetchConversations(conversationIds);

            indexer.updateSearchSync(conversations);
        }
    }

    private List<Conversation> fetchConversations(List<String> conversationIds) {
        List<Conversation> conversations = new ArrayList<>();

        for (String conversationId : conversationIds) {
            try {
                MutableConversation conversation = conversationRepository.getById(conversationId);

                // Might be null for very old conversation that have been removed by the cleanup job while the indexer was running
                if (conversation != null) {
                    conversations.add(conversation);
                }
            } catch (Exception e) {
                LOG.warn("Indexer could not load conversation '{}' from repository - skipping it", conversationId, e);
                FAILED_IDX.info(conversationId);
            }
        }

        if (conversations.size() > 0) {
            LOG.trace("Fetch {} conversations from {} to {} completed", conversationIds.size(), conversationIds.get(0), conversationIds.get(conversationIds.size() - 1));
        }

        return conversations;
    }
}