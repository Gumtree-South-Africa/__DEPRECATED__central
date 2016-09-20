package com.ecg.replyts.core.runtime.migrator;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MigrationChunkHandler {

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("migrate-chunk");
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-chunk");

    private final HybridConversationRepository conversationRepository;
    private final int maxChunkSize;

    private static final Logger LOG = LoggerFactory.getLogger(MigrationChunkHandler.class);
    private static final Logger FAILED_CONVERSATION_IDS = LoggerFactory.getLogger("FailedToFetchConversations");

    public MigrationChunkHandler(HybridConversationRepository conversationRepository, int maxChunkSize) {
        this.conversationRepository = conversationRepository;
        this.maxChunkSize = maxChunkSize;
    }

    public void migrateChunk(List<String> conversationIds) {
        if (conversationIds.size() > maxChunkSize) {
            LOG.info("Partitioning conversation list with {} elements into chunks of size {}", conversationIds.size(), maxChunkSize);
        }
        List<List<String>> partitions = Lists.partition(conversationIds, maxChunkSize);

        for (List<String> partition : partitions) {
            migrateChunkPartition(partition);
        }
    }

    private void migrateChunkPartition(List<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return;
        }
        try (Timer.Context timer = OVERALL_TIMER.time()) {
            fetchConversations(conversationIds);
        }
    }

    public void fetchConversations(List<String> conversationIds) {
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
                LOG.error(String.format("Migrator could not load conversation {} from repository - skipping it", convId), e);
                FAILED_CONVERSATION_IDS.info(convId);
            }
        }
        if (conversations.size() > 0) {

            LOG.trace("Fetch {} conversations from {} to {} completed", conversationIds.size(), conversationIds.get(0), conversationIds.get(conversationIds.size() - 1));
        }
        if (conversations.size() != conversationIds.size()) {

            LOG.warn("At least some conversation IDs were not found in the database, {} conversations expected, but only {} retrieved",
                    conversationIds.size(), conversations.size());
        }
    }


}